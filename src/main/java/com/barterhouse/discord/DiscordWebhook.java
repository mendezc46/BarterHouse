package com.barterhouse.discord;

import com.barterhouse.util.LoggerUtil;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Cliente para enviar mensajes a Discord mediante Webhooks
 */
public class DiscordWebhook {
    
    private final String webhookUrl;
    
    public DiscordWebhook(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }
    
    /**
     * Envía una imagen con texto a Discord
     */
    public void sendImageWithMessage(File imageFile, String message) {
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.equals("URL_DEL_WEBHOOK_AQUI")) {
            LoggerUtil.info("Discord webhook not configured, skipping notification");
            return;
        }
        
        try {
            String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
            URL url = new URL(webhookUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            connection.setRequestProperty("User-Agent", "BarterHouse-Minecraft-Mod");
            
            try (OutputStream os = connection.getOutputStream();
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8), true)) {
                
                // Parte del mensaje (payload_json)
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"payload_json\"\r\n");
                writer.append("Content-Type: application/json\r\n\r\n");
                writer.append("{\"content\":\"").append(escapeJson(message)).append("\"}\r\n");
                writer.flush();
                
                // Parte de la imagen
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                      .append(imageFile.getName()).append("\"\r\n");
                writer.append("Content-Type: image/png\r\n\r\n");
                writer.flush();
                
                // Escribir bytes de la imagen
                try (FileInputStream fis = new FileInputStream(imageFile)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                    os.flush();
                }
                
                writer.append("\r\n");
                writer.append("--").append(boundary).append("--\r\n");
                writer.flush();
            }
            
            int responseCode = connection.getResponseCode();
            if (responseCode == 200 || responseCode == 204) {
                LoggerUtil.info("Discord notification sent successfully");
            } else {
                LoggerUtil.error("Discord webhook failed with code: " + responseCode);
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        LoggerUtil.error("Discord response: " + line);
                    }
                }
            }
            
            connection.disconnect();
            
        } catch (Exception e) {
            LoggerUtil.error("Error sending Discord notification: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
