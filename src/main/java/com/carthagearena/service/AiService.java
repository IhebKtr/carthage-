package com.carthagearena.service;

import com.carthagearena.util.AppConfig;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Service IA (Groq AI) - traduit depuis le service Symfony AiService.php
 *
 * Fonctions :
 *  - generateMerchDescription()  : génère une description marketing via LLaMA 3.3
 *  - calculateDynamicPrice()     : calcule un prix dynamique (rareté + tendance)
 *  - fraudScore()                : score de fraude (quantité / montant anormal)
 *
 * Utilise OkHttp (équiv. HttpClientInterface Symfony) + Gson (JSON)
 */
public class AiService {

    private static final String MODEL       = "llama-3.3-70b-versatile";
    private static final MediaType JSON_MT  = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final Gson gson;
    private final String apiKey;
    private final String endpoint;  // base URL (ex: https://api.groq.com/openai/v1)

    // ─── Constructeur ─────────────────────────────────────────────────────────

    public AiService() {
        // Équivalent de $_ENV['GROQ_API_KEY'] ?? ''  avec exception si absent
        this.apiKey = AppConfig.getRequired("GROQ_API_KEY");
        this.endpoint = AppConfig.get("GROQ_API_URL", "https://api.groq.com/openai/v1")
                .stripTrailing();

        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        this.gson = new Gson();
    }

    // ─── generateMerchDescription() ──────────────────────────────────────────
    // Équivalent PHP : generateMerchDescription(string $name, string $type, int $price)

    /**
     * Génère une description marketing épique pour un produit Merch via Groq AI.
     *
     * @param name  Nom du produit
     * @param type  Type (shirt, cap, jersey, ...)
     * @param price Prix en centimes
     * @return Description générée par l'IA
     * @throws RuntimeException si l'API est inaccessible ou retourne une erreur
     */
    public String generateMerchDescription(String name, String type, int price) {
        String url = endpoint.stripTrailing() + "/chat/completions";

        // Construction du body JSON (équiv. json => [...] dans Symfony)
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content",
                "Create an epic gaming marketplace description for a " + type +
                " item called " + name + " priced at " + price + " coins.");

        JsonArray messages = new JsonArray();
        messages.add(message);

        JsonObject body = new JsonObject();
        body.addProperty("model", MODEL);
        body.add("messages", messages);

        RequestBody requestBody = RequestBody.create(gson.toJson(body), JSON_MT);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new RuntimeException(
                        "Groq API error " + response.code() + ": " +
                        (response.body() != null ? response.body().string() : "no body"));
            }

            String responseJson = response.body().string();
            JsonObject data = gson.fromJson(responseJson, JsonObject.class);

            // data['choices'][0]['message']['content']
            return data.getAsJsonArray("choices")
                       .get(0).getAsJsonObject()
                       .getAsJsonObject("message")
                       .get("content").getAsString();

        } catch (IOException e) {
            // Équiv. PHP : throw new RuntimeException('Groq API request failed: ' . $e->getMessage())
            throw new RuntimeException("Groq API request failed: " + e.getMessage(), e);
        }
    }

    // ─── calculateDynamicPrice() ──────────────────────────────────────────────
    // Équivalent PHP : calculateDynamicPrice(int $basePrice, int $stock): float

    /**
     * Calcule un prix dynamique basé sur la rareté et la tendance marché.
     *
     * @param basePrice Prix de base (en centimes)
     * @param stock     Quantité en stock
     * @return Prix dynamique (double)
     */
    public double calculateDynamicPrice(int basePrice, int stock) {
        double rarity = stock < 10 ? 0.5 : 0.1;               // rare si stock < 10
        double trend  = (Math.random() * 100) / 100;           // tendance aléatoire [0,1]
        return basePrice * (1 + rarity + trend);
    }

    // ─── fraudScore() ─────────────────────────────────────────────────────────
    // Équivalent PHP : fraudScore(int $quantity, int $totalPrice): float

    /**
     * Score de risque de fraude entre 0.0 (sûr) et 1.0 (suspect).
     * Règles :
     *  - quantité > 10 → score 0.9 (très suspect)
     *  - total > 10000 centimes → score 0.8
     *  - sinon → score 0.1 (normal)
     *
     * @param quantity   Nombre d'articles commandés
     * @param totalPrice Prix total en centimes
     * @return Score de fraude
     */
    public double fraudScore(int quantity, int totalPrice) {
        if (quantity > 10)    return 0.9;
        if (totalPrice > 10000) return 0.8;
        return 0.1;
    }

    /**
     * Retourne une description lisible du score de fraude.
     */
    public String fraudScoreLabel(double score) {
        if (score >= 0.8) return "🔴 Suspect (" + (int)(score * 100) + "%)";
        if (score >= 0.5) return "🟡 Attention (" + (int)(score * 100) + "%)";
        return "🟢 Normal (" + (int)(score * 100) + "%)";
    }
}
