package com.carthage.services.api;

import com.carthage.config.Config;
import com.carthage.entity.Skin;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

public class StripeService {

    public StripeService() {
        String secretKey = Config.get("STRIPE_SECRET_KEY");
        if (secretKey != null) {
            Stripe.apiKey = secretKey;
        } else {
            System.err.println("Stripe secret key not found in environment.");
        }
    }

    public String createCheckoutSession(Skin skin, String successUrl, String cancelUrl) {
        if (Stripe.apiKey == null) {
            System.err.println("Stripe is not initialized.");
            return null;
        }
        
        try {
            SessionCreateParams params =
                SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(cancelUrl)
                    .addLineItem(
                        SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(
                                SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency("usd")
                                    .setUnitAmount((long) (skin.getPrice() * 100)) // Stripe expects amount in cents
                                    .setProductData(
                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName(skin.getName())
                                            .setDescription(skin.getDescription() != null ? skin.getDescription() : "Skin from Carthage")
                                            // .addImage(skin.getImageUrl()) // Uncomment if you want image in checkout
                                            .build())
                                    .build())
                            .build())
                    .build();

            Session session = Session.create(params);
            return session.getUrl();
        } catch (StripeException e) {
            System.err.println("Error creating Stripe checkout session: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
