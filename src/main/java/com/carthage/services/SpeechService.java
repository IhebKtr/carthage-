package com.carthage.services;

import javafx.application.Platform;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.*;
import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class SpeechService {

    private static final String MODEL_URL = "https://alphacephei.com/vosk/models/vosk-model-small-fr-0.22.zip";
    private static final String MODEL_DIR = "models";
    private static final String MODEL_NAME = "vosk-model-small-fr-0.22";

    private Model model;
    private TargetDataLine microphone;
    private Thread recognitionThread;
    private volatile boolean isListening = false;

    public interface SpeechCallback {
        void onSpeechRecognized(String text);
        void onStatusUpdate(String status);
        void onError(String error);
    }

    public void initModel(SpeechCallback callback) {
        new Thread(() -> {
            try {
                File modelDir = new File(MODEL_DIR, MODEL_NAME);
                if (!modelDir.exists()) {
                    Platform.runLater(() -> callback.onStatusUpdate("Téléchargement du modèle vocal (40MB)..."));
                    downloadAndExtractModel();
                }
                
                Platform.runLater(() -> callback.onStatusUpdate("Chargement du modèle..."));
                model = new Model(modelDir.getAbsolutePath());
                Platform.runLater(() -> callback.onStatusUpdate("Prêt !"));
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> callback.onError("Erreur d'initialisation : " + e.getMessage()));
            }
        }).start();
    }

    private void downloadAndExtractModel() throws IOException {
        File dir = new File(MODEL_DIR);
        if (!dir.exists()) dir.mkdirs();

        File zipFile = new File(MODEL_DIR, "model.zip");
        
        // Download
        try (InputStream in = new URL(MODEL_URL).openStream();
             FileOutputStream out = new FileOutputStream(zipFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }

        // Extract
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File newFile = new File(MODEL_DIR, entry.getName());
                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    newFile.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
        zipFile.delete(); // cleanup
    }

    public void startListening(SpeechCallback callback) {
        if (model == null) {
            callback.onError("Le modèle n'est pas encore prêt.");
            return;
        }

        AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        if (!AudioSystem.isLineSupported(info)) {
            callback.onError("Microphone non supporté.");
            return;
        }

        try {
            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
            microphone.start();
            isListening = true;

            recognitionThread = new Thread(() -> {
                try (Recognizer recognizer = new Recognizer(model, 16000)) {
                    byte[] b = new byte[4096];
                    Platform.runLater(() -> callback.onStatusUpdate("Écoute en cours... Parlez maintenant"));

                    while (isListening) {
                        int numBytesRead = microphone.read(b, 0, 1024);
                        if (numBytesRead >= 0) {
                            if (recognizer.acceptWaveForm(b, numBytesRead)) {
                                String result = recognizer.getResult();
                                // Parse JSON: { "text" : "bonjour" }
                                String text = extractText(result);
                                if (!text.isEmpty()) {
                                    Platform.runLater(() -> callback.onSpeechRecognized(text));
                                }
                            }
                        }
                    }
                    
                    // Final result when stopped
                    String result = recognizer.getFinalResult();
                    String text = extractText(result);
                    if (!text.isEmpty()) {
                        Platform.runLater(() -> callback.onSpeechRecognized(text));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> callback.onError("Erreur d'écoute : " + e.getMessage()));
                } finally {
                    stopListening();
                    Platform.runLater(() -> callback.onStatusUpdate("Arrêté"));
                }
            });
            recognitionThread.start();
            
        } catch (LineUnavailableException e) {
            e.printStackTrace();
            callback.onError("Microphone indisponible.");
        }
    }

    public void stopListening() {
        isListening = false;
        if (microphone != null) {
            microphone.stop();
            microphone.close();
            microphone = null;
        }
    }

    private String extractText(String json) {
        // Very basic JSON parsing to avoid heavy libraries
        String key = "\"text\" : \"";
        int start = json.indexOf(key);
        if (start == -1) return "";
        start += key.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return "";
        return json.substring(start, end).trim();
    }
}
