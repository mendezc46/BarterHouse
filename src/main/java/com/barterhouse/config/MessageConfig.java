package com.barterhouse.config;

import com.barterhouse.util.LoggerUtil;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestor de configuración de mensajes del mod.
 * Carga los mensajes desde archivo YAML en la carpeta del servidor.
 */
public class MessageConfig {
    
    private static MessageConfig instance;
    private final Map<String, String> messages;
    private Path messagesFile;
    
    private MessageConfig() {
        this.messages = new HashMap<>();
        // El archivo se inicializará cuando se tenga acceso al mundo
    }
    
    /**
     * Obtiene la instancia única del config.
     */
    public static synchronized MessageConfig getInstance() {
        if (instance == null) {
            instance = new MessageConfig();
        }
        return instance;
    }
    
    /**
     * Inicializa el config con el nivel del servidor.
     */
    public void initializeWithLevel(net.minecraft.world.level.Level level) {
        if (this.messagesFile == null && level != null) {
            try {
                Path worldPath = level.getServer().getServerDirectory().toPath();
                Path barterhouseDir = worldPath.resolve("barterhouse");
                Files.createDirectories(barterhouseDir);
                
                this.messagesFile = barterhouseDir.resolve("messages.yaml");
                
                // Si no existe, crear con valores por defecto
                if (!Files.exists(messagesFile)) {
                    createDefaultMessagesFile();
                    LoggerUtil.info("Archivo messages.yaml creado en: " + messagesFile.toAbsolutePath());
                }
                
                loadMessages();
            } catch (IOException e) {
                LoggerUtil.error("Error al inicializar archivo de mensajes: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Crea el archivo messages.yaml con valores por defecto.
     */
    private void createDefaultMessagesFile() throws IOException {
        String defaultYaml = "menu:\n" +
            "  offers_title: \"§7Lista de Ofertas\"\n" +
            "  create_title: \"§7Crear Oferta\"\n" +
            "  delete_title: \"§7Borrar Mis Ofertas\"\n" +
            "  search_title: \"§7Resultados de Búsqueda\"\n" +
            "  quantity_title: \"§7Selecciona la cantidad\"\n" +
            "  confirm_title: \"§7Confirmar Oferta\"\n" +
            "\n" +
            "buttons:\n" +
            "  create: \"§a§lCREAR\"\n" +
            "  delete: \"§c§lBORRAR\"\n" +
            "  accept: \"§a§lACEPTAR\"\n" +
            "  cancel: \"§c§lCANCELAR\"\n" +
            "  back: \"§e§lVOLVER\"\n" +
            "\n" +
            "errors:\n" +
            "  offer_not_found: \"§cOferta no encontrada\"\n" +
            "  no_items_found: \"§c¡No se encontraron items con '{search}'!\"\n" +
            "  no_search_text: \"§c¡Debes escribir algo en el letrero!\"\n" +
            "  offered_item_error: \"§cError: No se encontró el item ofrecido\"\n" +
            "  requested_item_error: \"§cError: No se encontró el item solicitado\"\n" +
            "  sign_editor_failed: \"§cError al abrir el editor de letrero\"\n" +
            "  insufficient_items_title: \"§c§l❌ NO TIENES EL ITEM REQUERIDO\"\n" +
            "  insufficient_items_separator: \"========================================\"\n" +
            "  insufficient_items_item: \"§7Item necesario: §e{item}\"\n" +
            "  insufficient_items_required: \"§7Cantidad requerida: §e{required}\"\n" +
            "  insufficient_items_have: \"§7Cantidad que tienes: §c{have}\"\n" +
            "  insufficient_items_missing: \"§7Te faltan: §c§l{missing}x {item}\"\n" +
            "  accept_error: \"§cError al aceptar oferta\"\n" +
            "\n" +
            "success:\n" +
            "  offer_created: \"§a¡Oferta creada exitosamente!\"\n" +
            "  offer_accepted: \"§a§l¡Oferta aceptada!\"\n" +
            "  offer_received: \"§7Has recibido: §e{count}x {item}\"\n" +
            "  offer_deleted: \"§a¡Oferta eliminada!\"\n" +
            "  inventory_full: \"§eInventario lleno, item dropeado en el suelo\"\n" +
            "\n" +
            "info:\n" +
            "  sign_editor_opened: \"§7Editor de letrero abierto. Escribe el nombre del item a buscar.\"\n" +
            "  found_items: \"§7Se encontraron {count} items\"\n";
        
        Files.write(messagesFile, defaultYaml.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Carga los mensajes desde el archivo YAML.
     */
    private void loadMessages() {
        if (messagesFile == null || !Files.exists(messagesFile)) {
            LoggerUtil.error("Archivo de mensajes no encontrado: " + messagesFile);
            return;
        }
        
        try {
            InputStream inputStream = Files.newInputStream(messagesFile);
            InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            Yaml yaml = new Yaml();
            Map<String, Object> root = yaml.load(reader);
            reader.close();
            inputStream.close();
            
            // Flatten YAML into a map with dot notation keys
            if (root != null) {
                flattenMap(root, "", messages);
                LoggerUtil.info("Mensajes cargados de: " + messagesFile.toAbsolutePath() + " (" + messages.size() + " mensajes)");
            }
            
        } catch (Exception e) {
            LoggerUtil.error("Error al cargar messages.yaml: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Convierte YAML anidado a un mapa con notación de puntos.
     * Ej: {"menu": {"title": "Menu"}} -> {"menu.title": "Menu"}
     */
    @SuppressWarnings("unchecked")
    private void flattenMap(Map<String, Object> map, String prefix, Map<String, String> result) {
        for (String key : map.keySet()) {
            String newKey = prefix.isEmpty() ? key : prefix + "." + key;
            Object value = map.get(key);
            
            if (value instanceof Map) {
                flattenMap((Map<String, Object>) value, newKey, result);
            } else if (value != null) {
                result.put(newKey, String.valueOf(value));
            }
        }
    }
    
    /**
     * Obtiene un mensaje por su clave.
     * Las claves usan notación de puntos: "menu.title"
     * 
     * @param key Clave del mensaje
     * @return Mensaje con color y formato
     */
    public String get(String key) {
        String message = messages.getOrDefault(key, "§c[Mensaje no encontrado: " + key + "]");
        return message;
    }
    
    /**
     * Obtiene un mensaje y reemplaza placeholders.
     * Ejemplo: get("error.insufficient_items_item", "item", "Diamante")
     * 
     * @param key Clave del mensaje
     * @param placeholders Pares de clave-valor para reemplazar
     * @return Mensaje con placeholders reemplazados
     */
    public String get(String key, Object... placeholders) {
        String message = get(key);
        
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                String placeholder = "{" + placeholders[i] + "}";
                String value = String.valueOf(placeholders[i + 1]);
                message = message.replace(placeholder, value);
            }
        }
        
        return message;
    }
    
    /**
     * Recarga los mensajes desde el archivo.
     */
    public void reload() {
        messages.clear();
        loadMessages();
    }
}
