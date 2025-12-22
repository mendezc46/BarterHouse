package com.barterhouse.config;

import com.barterhouse.util.LoggerUtil;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuración de Discord para webhooks
 */
public class DiscordConfig {
    
    private static DiscordConfig instance;
    private Path configFile;
    private boolean enabled = false;
    private String webhookUrl = "";
    
    private DiscordConfig() {
    }
    
    public static synchronized DiscordConfig getInstance() {
        if (instance == null) {
            instance = new DiscordConfig();
        }
        return instance;
    }
    
    public void initializeWithLevel(net.minecraft.world.level.Level level) {
        if (this.configFile == null && level != null) {
            try {
                Path worldPath = level.getServer().getServerDirectory().toPath();
                Path barterhouseDir = worldPath.resolve("barterhouse");
                Files.createDirectories(barterhouseDir);
                
                this.configFile = barterhouseDir.resolve("config.yml");
                
                if (!Files.exists(configFile)) {
                    createDefaultConfigFile();
                    LoggerUtil.info("Archivo config.yml creado en: " + configFile.toAbsolutePath());
                }
                
                loadConfig();
            } catch (IOException e) {
                LoggerUtil.error("Error al inicializar configuración de Discord: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    private void createDefaultConfigFile() throws IOException {
        String defaultYaml = "# Configuración de Discord Webhook\n" +
            "discord:\n" +
            "  # Activar o desactivar notificaciones de Discord\n" +
            "  enabled: false\n" +
            "  \n" +
            "  # URL del webhook de Discord\n" +
            "  # Para obtener una URL de webhook:\n" +
            "  # 1. Ve a tu servidor de Discord\n" +
            "  # 2. Click derecho en el canal donde quieres las notificaciones\n" +
            "  # 3. Editar Canal > Integraciones > Webhooks > Nuevo Webhook\n" +
            "  # 4. Copia la URL del webhook y pégala abajo\n" +
            "  webhook_url: \"URL_DEL_WEBHOOK_AQUI\"\n";
        
        Files.write(configFile, defaultYaml.getBytes(StandardCharsets.UTF_8));
    }
    
    @SuppressWarnings("unchecked")
    private void loadConfig() {
        try (InputStream input = Files.newInputStream(configFile)) {
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(input);
            
            if (data != null && data.containsKey("discord")) {
                Map<String, Object> discordConfig = (Map<String, Object>) data.get("discord");
                
                if (discordConfig.containsKey("enabled")) {
                    this.enabled = (Boolean) discordConfig.get("enabled");
                }
                
                if (discordConfig.containsKey("webhook_url")) {
                    this.webhookUrl = (String) discordConfig.get("webhook_url");
                }
                
                LoggerUtil.info("Discord config loaded - Enabled: " + enabled);
            }
            
        } catch (Exception e) {
            LoggerUtil.error("Error cargando configuración de Discord: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public boolean isEnabled() {
        return enabled && webhookUrl != null && !webhookUrl.isEmpty() && !webhookUrl.equals("URL_DEL_WEBHOOK_AQUI");
    }
    
    public String getWebhookUrl() {
        return webhookUrl;
    }
}
