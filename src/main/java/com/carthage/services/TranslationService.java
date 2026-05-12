package com.carthage.services;

import org.json.JSONArray;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class TranslationService {

    private static final String API_URL = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=%s&dt=t&q=%s";

    public String translate(String text, String targetLang) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        try {
            String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8.toString());
            String url = String.format(API_URL, targetLang, encodedText);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONArray jsonArray = new JSONArray(response.body());
                JSONArray resultParts = jsonArray.getJSONArray(0);
                
                StringBuilder translatedText = new StringBuilder();
                for (int i = 0; i < resultParts.length(); i++) {
                    JSONArray part = resultParts.getJSONArray(i);
                    translatedText.append(part.getString(0));
                }
                
                return translatedText.toString();
            } else {
                System.err.println("Translation failed with status: " + response.statusCode());
                return "Erreur de traduction";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Erreur de traduction";
        }
    }
}
