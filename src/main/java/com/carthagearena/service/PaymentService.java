package com.carthagearena.service;

import com.carthagearena.model.Merch;
import com.carthagearena.model.Order;
import com.carthagearena.util.AppConfig;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;

import java.util.HashMap;
import java.util.Map;

/**
 * Service de paiement Stripe - traduit depuis PaymentService.php (Symfony)
 *
 * Fonctions :
 *  - createCheckoutSession() : crée une session Stripe Checkout
 *  - handleWebhook()         : vérifie et traite les webhooks Stripe
 *  - fulfillOrder()          : finalise la commande après paiement
 *
 * Note : Dans JavaFX, le webhook ne s'applique pas (pas de serveur HTTP).
 * On simule le succès du paiement directement pour l'environnement desktop.
 */
public class PaymentService {

    private final OrderService orderService;
    private final String webhookSecret;
    private final String publishableKey;

    // ─── Constructeur ─────────────────────────────────────────────────────────

    public PaymentService(OrderService orderService) {
        this.orderService = orderService;

        // Équiv. PHP : $this->stripeSecretKey = ...
        String secretKey = AppConfig.getRequired("STRIPE_SECRET_KEY");
        this.webhookSecret = AppConfig.get("STRIPE_WEBHOOK_SECRET", "");
        this.publishableKey = AppConfig.get("STRIPE_PUBLISHABLE_KEY", "");

        // Initialise le SDK Stripe (équiv. new StripeClient($key) en PHP)
        Stripe.apiKey = secretKey;
    }

    // ─── createCheckoutSession() ──────────────────────────────────────────────
    // Équivalent PHP : createCheckoutSession(Skin|Merch $item, User $user, string $itemType)

    /**
     * Crée une session Stripe Checkout pour un produit Merch.
     *
     * @param merch    Le produit à acheter
     * @param userId   ID de l'utilisateur
     * @param userEmail Email de l'utilisateur
     * @return URL de redirection vers la page Stripe Checkout
     * @throws StripeException en cas d'erreur Stripe
     */
    public String createCheckoutSession(Merch merch, int userId, String userEmail)
            throws StripeException {

        // Équiv. PHP : (int) round($item->getPrice() * 100)
        // Ici le prix est déjà en centimes, pas besoin de * 100
        long priceInCents = merch.getPrice();

        // Construction des paramètres (équiv. $this->stripe->checkout->sessions->create([...]))
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setCustomerEmail(userEmail)
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                // URL de succès (redirige vers un site bidon car c'est une app bureau)
                .setSuccessUrl("https://example.com/?status=success&session_id={CHECKOUT_SESSION_ID}")
                // URL d'annulation
                .setCancelUrl("https://example.com/?status=cancel")
                // Métadonnées (équiv. 'metadata' => [...] en PHP)
                .putMetadata("item_id",   merch.getId().toString())
                .putMetadata("item_type", "merch")
                .putMetadata("user_id",   String.valueOf(userId))
                // Produit
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("eur")
                                .setUnitAmount(priceInCents)
                                .setProductData(
                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                .setName(merch.getName())
                                                .setDescription(
                                                        merch.getDescription() != null
                                                        ? merch.getDescription() : "")
                                                .build())
                                .build())
                        .build())
                .build();

        Session session = Session.create(params);
        return session.getUrl(); // URL à ouvrir dans le navigateur
    }

    // ─── handleWebhook() ──────────────────────────────────────────────────────
    // Équivalent PHP : handleWebhook(Request $request): bool
    // Note Java : dans une app desktop, ce webhook serait traité par un petit
    // serveur HTTP embarqué (ex: com.sun.net.httpserver). Cette méthode
    // peut être appelée depuis un endpoint Webhook séparé.

    /**
     * Vérifie la signature d'un webhook Stripe et traite l'événement.
     *
     * @param payload    Corps brut de la requête HTTP
     * @param sigHeader  Valeur du header "Stripe-Signature"
     * @return true si traité avec succès, false sinon
     */
    public boolean handleWebhook(String payload, String sigHeader) {
        Event event;

        try {
            // Équiv. PHP : Webhook::constructEvent($payload, $sigHeader, $this->stripeWebhookSecret)
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            System.err.println("❌ Signature Stripe invalide : " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("❌ Payload Stripe invalide : " + e.getMessage());
            return false;
        }

        // Équiv. PHP : if ($event->type === 'checkout.session.completed')
        if ("checkout.session.completed".equals(event.getType())) {
            Session session = (Session) event.getDataObjectDeserializer()
                    .getObject().orElse(null);
            if (session != null) {
                fulfillOrder(session);
            }
        }

        return true;
    }

    // ─── fulfillOrder() ───────────────────────────────────────────────────────
    // Équivalent PHP : fulfillOrder(Session $session): void

    /**
     * Finalise une commande après paiement Stripe réussi.
     * Met à jour le statut de la commande en PAID.
     */
    private void fulfillOrder(Session session) {
        Map<String, String> metadata = session.getMetadata();
        String itemId   = metadata.get("item_id");
        String itemType = metadata.get("item_type");
        String userId   = metadata.get("user_id");

        if (itemId == null || itemType == null || userId == null) {
            System.err.println("⚠️ Métadonnées de session Stripe incomplètes");
            return;
        }

        System.out.println("✅ Paiement confirmé pour item=" + itemId
                + " type=" + itemType + " user=" + userId);

        // Marquer la commande PENDING correspondante comme PAID
        try {
            java.util.List<Order> orders = orderService.findByUserId(Integer.parseInt(userId));
            orders.stream()
                  .filter(o -> o.getStatus() == Order.Status.PENDING)
                  .findFirst()
                  .ifPresent(order -> {
                      try {
                          orderService.payOrder(order.getId().toString());
                          System.out.println("✅ Commande " + order.getId() + " marquée comme PAID");
                      } catch (java.sql.SQLException e) {
                          System.err.println("❌ Erreur mise à jour commande : " + e.getMessage());
                      }
                  });
        } catch (java.sql.SQLException e) {
            System.err.println("❌ Erreur récupération commandes : " + e.getMessage());
        }
    }

    // ─── Simulation paiement (mode test JavaFX desktop) ───────────────────────

    /**
     * Simule un paiement réussi en mode test (sans redirection navigateur).
     * Marque directement une commande comme PAID.
     *
     * @param orderId UUID de la commande à payer
     */
    public void simulatePayment(String orderId) throws java.sql.SQLException {
        orderService.payOrder(orderId);
        System.out.println("✅ [Simulation Stripe] Commande " + orderId + " payée");
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public String getPublishableKey() {
        return publishableKey;
    }
}
