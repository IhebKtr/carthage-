package com.carthage.services.api;

import com.carthage.config.Config;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class RawgService {

    private static final String BASE_URL = "https://api.rawg.io/api/games";
    private final String apiKey;
    private final HttpClient httpClient;
    private final Gson gson;

    public RawgService() {
        this.apiKey = Config.get("RAWG_API_KEY");
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
        
        if (this.apiKey == null) {
            System.err.println("RAWG API Key not found in environment.");
        }
    }

    public RawgGame searchGame(String query) {
        if (apiKey == null) return null;

        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = BASE_URL + "?key=" + apiKey + "&search=" + encodedQuery + "&page_size=1";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
                JsonArray results = jsonResponse.getAsJsonArray("results");

                if (results != null && results.size() > 0) {
                    JsonObject gameData = results.get(0).getAsJsonObject();
                    
                    RawgGame game = new RawgGame();
                    game.setName(gameData.has("name") && !gameData.get("name").isJsonNull() ? gameData.get("name").getAsString() : "Unknown");
                    game.setBackgroundImage(gameData.has("background_image") && !gameData.get("background_image").isJsonNull() ? gameData.get("background_image").getAsString() : null);
                    
                    // Note: Description requires a second API call to the specific game ID endpoint in RAWG
                    // For now, we return basic info
                    int id = gameData.get("id").getAsInt();
                    game.setDescription(fetchGameDescription(id));
                    
                    return game;
                }
            } else {
                System.err.println("RAWG API returned status: " + response.statusCode());
            }

        } catch (Exception e) {
            System.err.println("Error searching RAWG: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    private String fetchGameDescription(int gameId) {
        try {
            String url = BASE_URL + "/" + gameId + "?key=" + apiKey;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
                return jsonResponse.has("description_raw") && !jsonResponse.get("description_raw").isJsonNull() ? 
                       jsonResponse.get("description_raw").getAsString() : "No description available.";
            }
        } catch (Exception e) {
            System.err.println("Error fetching RAWG description: " + e.getMessage());
        }
        return "No description available.";
    }

    public static class RawgGame {
        private String name;
        private String description;
        private String backgroundImage;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getBackgroundImage() { return backgroundImage; }
        public void setBackgroundImage(String backgroundImage) { this.backgroundImage = backgroundImage; }
    }
}
