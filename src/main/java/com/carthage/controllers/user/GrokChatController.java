package com.carthage.controllers.user;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

/**
 * Controller for the Grok AI chat overlay.
 * Restricts topics to: tournaments, gaming, esports, teams, Carthage Arena
 * platform.
 */
public class GrokChatController {

    // ── FXML injections ─────────────────────────────────────────────────────
    @FXML
    private ScrollPane scrollMessages;
    @FXML
    private VBox messagesBox;
    @FXML
    private HBox typingIndicator;
    @FXML
    private Label dotAnimLabel;
    @FXML
    private TextArea inputField;
    @FXML
    private Button btnSend;
    @FXML
    private Label lblStatus;

    // ── API config ───────────────────────────────────────────────────────────
    private static final String API_KEY = "AIzaSyBIpIlmRk70j4FkFakz48eWVO9ejqPRTbk";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent";

    /**
     * System prompt that strictly limits the AI to platform-relevant topics.
     */
    private static final String SYSTEM_PROMPT = "Tu es l'assistant IA officiel de Carthage Arena, une plateforme d'esports et de tournois de gaming. "
            +
            "Tu DOIS uniquement répondre aux questions concernant les sujets suivants : " +
            "- Les tournois (inscription, règles, calendrier, formats, brackets) " +
            "- Les jeux vidéo et l'esport (stratégies, titres, compétitions) " +
            "- Les équipes et joueurs (formation, rôles, communication) " +
            "- La plateforme Carthage Arena (fonctionnalités, profil, boutique, réclamations) " +
            "- Les skins, récompenses et la boutique in-game " +
            "Pour TOUTE autre question hors de ces sujets, réponds EXACTEMENT : " +
            "\"Je suis limité aux sujets liés aux tournois, jeux et à la plateforme Carthage Arena. " +
            "Pose-moi une question sur ces thèmes ! 🎮\" " +
            "Réponds en français de manière concise et utile. Utilise des emojis avec parcimonie.";

    // ── State ────────────────────────────────────────────────────────────────
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private Timeline dotTimeline;
    private int dotCount = 0;

    // ── Conversation history (for context) ──────────────────────────────────
    private final java.util.List<String[]> history = new java.util.ArrayList<>();

    // ────────────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        // Allow send with Enter (Shift+Enter = newline)
        inputField.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER && !e.isShiftDown()) {
                e.consume();
                onSendMessage();
            }
        });

        // Typing indicator dot animation
        dotTimeline = new Timeline(new KeyFrame(Duration.millis(400), e -> {
            dotCount = (dotCount + 1) % 4;
            dotAnimLabel.setText(".".repeat(dotCount == 0 ? 1 : dotCount));
        }));
        dotTimeline.setCycleCount(Timeline.INDEFINITE);

        // Welcome message
        addBotMessage("Bonjour ! Je suis votre assistant IA Carthage Arena propulsé par Gemini. 🎮\n" +
                "Je peux vous aider avec les **tournois**, **jeux**, **équipes** et la **plateforme**.\n" +
                "Comment puis-je vous aider ?");
    }

    // ── Send handler ─────────────────────────────────────────────────────────
    @FXML
    public void onSendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty())
            return;

        inputField.clear();
        addUserMessage(text);
        showTyping(true);
        btnSend.setDisable(true);

        // Keep last 6 exchanges for context (12 messages)
        history.add(new String[] { "user", text });
        if (history.size() > 12)
            history.remove(0);

        CompletableFuture
                .supplyAsync(() -> callGrokApi(history))
                .thenAcceptAsync(response -> {
                    history.add(new String[] { "assistant", response });
                    if (history.size() > 12)
                        history.remove(0);

                    Platform.runLater(() -> {
                        showTyping(false);
                        btnSend.setDisable(false);
                        addBotMessage(response);
                    });
                }, Platform::runLater)
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showTyping(false);
                        btnSend.setDisable(false);
                        addBotMessage("⚠️ Erreur de connexion. Vérifiez votre réseau et réessayez.");
                    });
                    return null;
                });
    }

    // ── API call ─────────────────────────────────────────────────────────────
    private String callGrokApi(java.util.List<String[]> conversationHistory) {
        try {
            StringBuilder contentsJson = new StringBuilder("[");

            for (int i = 0; i < conversationHistory.size(); i++) {
                String[] msg = conversationHistory.get(i);
                String role = msg[0].equals("user") ? "user" : "model";
                
                if (i > 0) contentsJson.append(",");
                
                contentsJson.append("{\"role\":\"").append(role)
                        .append("\",\"parts\":[{\"text\":").append(jsonString(msg[1]))
                        .append("}]}");
            }
            contentsJson.append("]");

            String body = "{"
                    + "\"systemInstruction\": {"
                    + "  \"parts\": [{\"text\":" + jsonString(SYSTEM_PROMPT) + "}]"
                    + "},"
                    + "\"contents\":" + contentsJson
                    + "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("X-goog-api-key", API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(body, java.nio.charset.StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());

            String rawBody = response.body();
            System.out.println("[Gemini API] status=" + response.statusCode());
            System.out.println("[Gemini API] body=" + rawBody);

            // Surface API-level errors gracefully
            if (response.statusCode() != 200) {
                String errMsg = extractErrorMessage(rawBody);
                return "⚠️ Erreur API (" + response.statusCode() + "): " + errMsg;
            }

            return parseGeminiResponse(rawBody);

        } catch (Exception e) {
            e.printStackTrace();
            return "⚠️ Impossible de contacter l'IA. Vérifiez votre connexion.";
        }
    }

    // ── JSON helpers ─────────────────────────────────────────────────────────
    private String jsonString(String s) {
        return "\"" + s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }

    private String parseGeminiResponse(String json) {
        try {
            int candidatesIdx = json.indexOf("\"candidates\"");
            if (candidatesIdx == -1)
                return extractErrorMessage(json);

            int contentIdx = json.indexOf("\"content\"", candidatesIdx);
            if (contentIdx == -1)
                return "Contenu introuvable.";

            int partsIdx = json.indexOf("\"parts\"", contentIdx);
            if (partsIdx == -1)
                return "Parties introuvables.";

            int textIdx = json.indexOf("\"text\"", partsIdx);
            if (textIdx == -1)
                return "Texte introuvable.";

            int colonPos = json.indexOf(':', textIdx + 6);
            if (colonPos == -1)
                return "Texte introuvable.";

            int valStart = colonPos + 1;
            while (valStart < json.length() && json.charAt(valStart) == ' ')
                valStart++;

            if (json.charAt(valStart) == '"') {
                return extractJsonString(json, valStart + 1).trim();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Erreur lors du traitement de la réponse.";
    }

    /**
     * Extract a JSON-escaped string starting at position {@code start} (after
     * opening quote).
     */
    private String extractJsonString(String json, int start) {
        StringBuilder sb = new StringBuilder();
        int i = start;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                switch (next) {
                    case 'n' -> {
                        sb.append('\n');
                        i += 2;
                    }
                    case 't' -> {
                        sb.append('\t');
                        i += 2;
                    }
                    case 'r' -> {
                        i += 2;
                    } // skip \r
                    case '"' -> {
                        sb.append('"');
                        i += 2;
                    }
                    case '\\' -> {
                        sb.append('\\');
                        i += 2;
                    }
                    case 'u' -> {
                        // Unicode escape sequence (u + 4 hex digits)
                        if (i + 5 < json.length()) {
                            String hex = json.substring(i + 2, i + 6);
                            try {
                                sb.append((char) Integer.parseInt(hex, 16));
                            } catch (NumberFormatException ex) {
                                sb.append('?');
                            }
                            i += 6;
                        } else {
                            i += 2;
                        }
                    }
                    default -> {
                        sb.append(next);
                        i += 2;
                    }
                }
            } else if (c == '"') {
                break; // closing quote
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    /**
     * Extract error message from API error JSON, e.g. {"error":{"message":"..."}}
     */
    private String extractErrorMessage(String json) {
        try {
            int errIdx = json.indexOf("\"error\"");
            if (errIdx == -1)
                return json.length() > 200 ? json.substring(0, 200) : json;
            int msgIdx = json.indexOf("\"message\"", errIdx);
            if (msgIdx == -1)
                return "Erreur API inconnue.";
            int col = json.indexOf(':', msgIdx + 9);
            int vs = col + 1;
            while (vs < json.length() && json.charAt(vs) == ' ')
                vs++;
            if (json.charAt(vs) == '"') {
                return extractJsonString(json, vs + 1);
            }
        } catch (Exception ignored) {
        }
        return "Erreur API inconnue.";
    }

    // ── UI helpers ───────────────────────────────────────────────────────────
    private void addUserMessage(String text) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_RIGHT);

        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(260);
        bubble.getStyleClass().add("grok-bubble-user");

        row.getChildren().add(bubble);
        messagesBox.getChildren().add(row);
        scrollToBottom();
    }

    private void addBotMessage(String text) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        // Bot avatar
        Label avatar = new Label("G");
        avatar.getStyleClass().add("grok-avatar");

        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(260);
        bubble.getStyleClass().add("grok-bubble-bot");

        row.getChildren().addAll(avatar, bubble);
        messagesBox.getChildren().add(row);
        scrollToBottom();
    }

    private void showTyping(boolean show) {
        typingIndicator.setVisible(show);
        typingIndicator.setManaged(show);
        if (show) {
            dotTimeline.play();
        } else {
            dotTimeline.stop();
        }
    }

    private void scrollToBottom() {
        Platform.runLater(() -> scrollMessages.setVvalue(1.0));
    }
}
