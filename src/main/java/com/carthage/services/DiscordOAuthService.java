package com.carthage.services;

import com.carthage.utils.EnvConfig;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiscordOAuthService {

    private static final String AUTH_BASE_URL = "https://discord.com/api/oauth2/authorize";
    private static final String TOKEN_URL = "https://discord.com/api/oauth2/token";
    private static final String USER_INFO_URL = "https://discord.com/api/users/@me";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int CALLBACK_TIMEOUT_SECONDS = 120;

    private final HttpClient httpClient;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public DiscordOAuthService() {
        EnvConfig config = EnvConfig.getInstance();
        this.clientId = config.getRequired("DISCORD_CLIENT_ID");
        this.clientSecret = config.getRequired("DISCORD_CLIENT_SECRET");
        this.redirectUri = config.getRequired("DISCORD_REDIRECT_URI");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public DiscordIdentity authenticate() throws OAuthException {
        validateRedirectUri();

        String state = generateState();
        URI authUri = buildAuthorizeUri(state);

        OAuthCallbackReceiver receiver = null;
        try {
            receiver = new OAuthCallbackReceiver(URI.create(redirectUri), state);
            receiver.start();

            openSystemBrowser(authUri);
            CallbackPayload callback = receiver.awaitCallback(CALLBACK_TIMEOUT_SECONDS);

            String accessToken = exchangeCodeForAccessToken(callback.code());
            return fetchDiscordIdentity(accessToken);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OAuthException("Erreur durant l'authentification Discord : " + e.getMessage(), e);
        } catch (IOException e) {
            throw new OAuthException("Erreur durant l'authentification Discord : " + e.getMessage(), e);
        } finally {
            if (receiver != null) {
                receiver.stop();
            }
        }
    }

    private void validateRedirectUri() throws OAuthException {
        URI uri;
        try {
            uri = URI.create(redirectUri);
        } catch (IllegalArgumentException e) {
            throw new OAuthException("DISCORD_REDIRECT_URI invalide.");
        }

        String host = uri.getHost();
        if (host == null || (!host.equals("localhost") && !host.equals("127.0.0.1"))) {
            throw new OAuthException("Le redirect URI Discord doit pointer vers localhost.");
        }
        if (uri.getPort() <= 0) {
            throw new OAuthException("Le redirect URI Discord doit inclure un port explicite.");
        }
    }

    private URI buildAuthorizeUri(String state) {
        String query = "client_id=" + encode(clientId) +
                "&redirect_uri=" + encode(redirectUri) +
                "&response_type=code" +
                "&scope=" + encode("identify email") +
                "&state=" + encode(state) +
                "&prompt=consent";

        return URI.create(AUTH_BASE_URL + "?" + query);
    }

    private String exchangeCodeForAccessToken(String code) throws OAuthException, IOException, InterruptedException {
        String body = "client_id=" + encode(clientId) +
                "&client_secret=" + encode(clientSecret) +
                "&grant_type=authorization_code" +
                "&code=" + encode(code) +
                "&redirect_uri=" + encode(redirectUri);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new OAuthException("Discord a refusé l'échange du code (HTTP " + response.statusCode() + ").");
        }

        String accessToken = extractJsonString(response.body(), "access_token");
        if (accessToken == null || accessToken.isBlank()) {
            throw new OAuthException("Réponse Discord invalide : access_token manquant.");
        }
        return accessToken;
    }

    private DiscordIdentity fetchDiscordIdentity(String accessToken) throws OAuthException, IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(USER_INFO_URL))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new OAuthException("Impossible de récupérer le profil Discord (HTTP " + response.statusCode() + ").");
        }

        String body = response.body();
        String id = extractJsonString(body, "id");
        String username = extractJsonString(body, "username");
        String globalName = extractJsonString(body, "global_name");
        String email = extractJsonString(body, "email");
        boolean verified = extractJsonBoolean(body, "verified");

        if (id == null || id.isBlank()) {
            throw new OAuthException("Réponse Discord invalide : id utilisateur manquant.");
        }
        if (email == null || email.isBlank()) {
            throw new OAuthException("Discord n'a pas fourni d'email. Vérifie le scope 'email'.");
        }

        String displayName = (globalName != null && !globalName.isBlank()) ? globalName : username;
        if (displayName == null || displayName.isBlank()) {
            displayName = "discord_user";
        }

        return new DiscordIdentity(id, email, displayName, verified);
    }

    private void openSystemBrowser(URI authUri) throws OAuthException {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            throw new OAuthException("Navigation web non supportée sur cette machine.");
        }
        try {
            Desktop.getDesktop().browse(authUri);
        } catch (IOException e) {
            throw new OAuthException("Impossible d'ouvrir le navigateur pour Discord OAuth.", e);
        }
    }

    private static String generateState() {
        byte[] random = new byte[24];
        SECURE_RANDOM.nextBytes(random);
        StringBuilder sb = new StringBuilder(random.length * 2);
        for (byte b : random) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String extractJsonString(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            return null;
        }
        return unescapeJsonString(matcher.group(1));
    }

    private static boolean extractJsonBoolean(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(true|false)");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() && Boolean.parseBoolean(matcher.group(1));
    }

    private static String unescapeJsonString(String value) {
        return value
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\/", "/");
    }

    public static class OAuthException extends Exception {
        public OAuthException(String message) {
            super(message);
        }

        public OAuthException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public record DiscordIdentity(String discordId, String email, String username, boolean emailVerified) {
        public DiscordIdentity {
            Objects.requireNonNull(discordId, "discordId");
            Objects.requireNonNull(email, "email");
            Objects.requireNonNull(username, "username");
        }
    }

    private record CallbackPayload(String code, String state) {}

    private static class OAuthCallbackReceiver {
        private final URI redirectUri;
        private final String expectedState;
        private final CountDownLatch latch = new CountDownLatch(1);
        private volatile CallbackPayload payload;
        private volatile String error;
        private HttpServer server;

        OAuthCallbackReceiver(URI redirectUri, String expectedState) {
            this.redirectUri = redirectUri;
            this.expectedState = expectedState;
        }

        void start() throws IOException {
            int port = redirectUri.getPort();
            String path = redirectUri.getPath();
            if (path == null || path.isBlank()) {
                path = "/";
            }

            server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
            String normalizedPath = path;
            server.createContext(normalizedPath, this::handleCallback);
            server.start();
        }

        CallbackPayload awaitCallback(int timeoutSeconds) throws OAuthException, InterruptedException {
            boolean completed = latch.await(timeoutSeconds, TimeUnit.SECONDS);
            if (!completed) {
                throw new OAuthException("Timeout : aucune réponse Discord reçue.");
            }
            if (error != null) {
                throw new OAuthException(error);
            }
            if (payload == null) {
                throw new OAuthException("Callback Discord invalide.");
            }
            if (!expectedState.equals(payload.state())) {
                throw new OAuthException("Échec de validation de l'état OAuth (state).");
            }
            return payload;
        }

        void stop() {
            if (server != null) {
                server.stop(0);
            }
        }

        private void handleCallback(HttpExchange exchange) throws IOException {
            String responseText = "Erreur OAuth.";
            int statusCode = 200;
            try {
                Map<String, String> query = parseQueryParams(exchange.getRequestURI().getRawQuery());
                String oauthError = query.get("error");
                if (oauthError != null) {
                    error = "Connexion Discord annulée (" + oauthError + ").";
                    responseText = "Connexion Discord annulée. Vous pouvez revenir à l'application.";
                } else {
                    String code = query.get("code");
                    String state = query.get("state");
                    if (code == null || code.isBlank() || state == null || state.isBlank()) {
                        error = "Paramètres OAuth manquants dans le callback Discord.";
                        statusCode = 400;
                        responseText = "Requête OAuth invalide.";
                    } else {
                        payload = new CallbackPayload(code, state);
                        responseText = "Connexion Discord réussie. Vous pouvez revenir à l'application.";
                    }
                }
            } catch (Exception e) {
                error = "Erreur pendant le callback Discord : " + e.getMessage();
                statusCode = 500;
                responseText = "Erreur interne lors de l'authentification.";
            } finally {
                byte[] bytes = responseText.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
                exchange.sendResponseHeaders(statusCode, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
                latch.countDown();
            }
        }

        private static Map<String, String> parseQueryParams(String rawQuery) {
            Map<String, String> params = new HashMap<>();
            if (rawQuery == null || rawQuery.isBlank()) {
                return params;
            }
            String[] pairs = rawQuery.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);
                String key = urlDecode(keyValue[0]);
                String value = keyValue.length > 1 ? urlDecode(keyValue[1]) : "";
                params.put(key, value);
            }
            return params;
        }

        private static String urlDecode(String value) {
            return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
        }
    }
}
