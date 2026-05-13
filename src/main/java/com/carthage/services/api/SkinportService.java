package com.carthage.services.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import org.brotli.dec.BrotliInputStream;

public class SkinportService {

    private static final String API_URL = "https://api.skinport.com/v1/items";
    private final HttpClient httpClient;
    private final Gson gson;

    public SkinportService() {
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
    }

    public List<SkinportItem> getItems(int limit) {
        List<SkinportItem> items = new ArrayList<>();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Accept-Encoding", "br") // Optional but good practice
                    .GET()
                    .build();

            HttpResponse<java.io.InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() == 200) {
                try (BrotliInputStream brotliStream = new BrotliInputStream(response.body());
                     Reader reader = new InputStreamReader(brotliStream, java.nio.charset.StandardCharsets.UTF_8)) {
                     
                    JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);
                    int count = 0;
                    for (JsonElement element : jsonArray) {
                        if (count >= limit) break;
                        JsonObject obj = element.getAsJsonObject();
                        
                        SkinportItem item = new SkinportItem();
                        item.setMarketHashName(obj.has("market_hash_name") && !obj.get("market_hash_name").isJsonNull() ? obj.get("market_hash_name").getAsString() : "Unknown");
                        item.setMinPrice(obj.has("min_price") && !obj.get("min_price").isJsonNull() ? obj.get("min_price").getAsDouble() : 0.0);
                        item.setSuggestedPrice(obj.has("suggested_price") && !obj.get("suggested_price").isJsonNull() ? obj.get("suggested_price").getAsDouble() : 0.0);
                        
                        items.add(item);
                        count++;
                    }
                }
            } else {
                System.err.println("Skinport API returned status: " + response.statusCode());
            }

        } catch (Exception e) {
            System.err.println("Error fetching data from Skinport: " + e.getMessage());
            e.printStackTrace();
        }
        return items;
    }

    public SkinportItem searchItem(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Accept-Encoding", "br")
                    .GET()
                    .build();

            HttpResponse<java.io.InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() == 200) {
                try (BrotliInputStream brotliStream = new BrotliInputStream(response.body());
                     Reader reader = new InputStreamReader(brotliStream, java.nio.charset.StandardCharsets.UTF_8)) {
                     
                    JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);
                    for (JsonElement element : jsonArray) {
                        JsonObject obj = element.getAsJsonObject();
                        if (obj.has("market_hash_name") && !obj.get("market_hash_name").isJsonNull()) {
                            String marketName = obj.get("market_hash_name").getAsString();
                            // Simple case-insensitive match
                            if (marketName.toLowerCase().contains(name.toLowerCase())) {
                                SkinportItem item = new SkinportItem();
                                item.setMarketHashName(marketName);
                                item.setMinPrice(obj.has("min_price") && !obj.get("min_price").isJsonNull() ? obj.get("min_price").getAsDouble() : 0.0);
                                item.setSuggestedPrice(obj.has("suggested_price") && !obj.get("suggested_price").isJsonNull() ? obj.get("suggested_price").getAsDouble() : 0.0);
                                return item;
                            }
                        }
                    }
                }
            } else {
                System.err.println("Skinport API returned status: " + response.statusCode());
            }

        } catch (Exception e) {
            System.err.println("Error searching Skinport: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // Helper class to represent the response
    public static class SkinportItem {
        private String marketHashName;
        private double minPrice;
        private double suggestedPrice;

        public String getMarketHashName() { return marketHashName; }
        public void setMarketHashName(String marketHashName) { this.marketHashName = marketHashName; }
        public double getMinPrice() { return minPrice; }
        public void setMinPrice(double minPrice) { this.minPrice = minPrice; }
        public double getSuggestedPrice() { return suggestedPrice; }
        public void setSuggestedPrice(double suggestedPrice) { this.suggestedPrice = suggestedPrice; }
        
        @Override
        public String toString() {
            return marketHashName + " - Min: $" + minPrice;
        }
    }
}
