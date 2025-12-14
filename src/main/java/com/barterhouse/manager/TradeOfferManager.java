package com.barterhouse.manager;

import com.barterhouse.api.TradeOffer;
import com.barterhouse.util.LoggerUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Gestor central de ofertas de trueque.
 * Maneja la creación, eliminación, búsqueda y persistencia de ofertas.
 */
public class TradeOfferManager {

    private static TradeOfferManager instance;
    private final Map<UUID, TradeOffer> activeOffers;
    private Path dataDirectory;
    private static final String OFFERS_FILE = "offers.json";

    private static final int SAVE_INTERVAL = 20 * 60; // Guardar cada 60 segundos (20 ticks * 60)
    private int saveCounter = 0;
    
    private Level serverLevel;

    /**
     * Constructor privado para el patrón Singleton.
     */
    private TradeOfferManager() {
        this.activeOffers = new HashMap<>();
        // El directorio se inicializará cuando se tenga acceso al mundo
    }

    /**
     * Obtiene la instancia única del manager.
     *
     * @return Instancia del TradeOfferManager
     */
    public static synchronized TradeOfferManager getInstance() {
        if (instance == null) {
            instance = new TradeOfferManager();
        }
        return instance;
    }
    
    /**
     * Inicializa el manager con el nivel del servidor.
     * Esto debe ser llamado al iniciar el servidor.
     */
    public void initializeWithLevel(Level level) {
        if (this.serverLevel == null && level != null) {
            this.serverLevel = level;
            initializeDataDirectory();
            loadOffers();
        }
    }

    /**
     * Inicializa el directorio de datos si no existe.
     */
    private void initializeDataDirectory() {
        if (serverLevel == null) {
            LoggerUtil.error("Cannot initialize data directory: server level is null");
            return;
        }
        
        try {
            // Obtener el directorio del mundo del servidor
            Path worldPath = serverLevel.getServer().getServerDirectory().toPath();
            // Crear directorio barterhouse dentro de la carpeta del mundo
            this.dataDirectory = worldPath.resolve("barterhouse");
            Files.createDirectories(dataDirectory);
            LoggerUtil.info("Data directory initialized at: " + dataDirectory.toAbsolutePath());
        } catch (IOException e) {
            LoggerUtil.error("Failed to create data directory: " + e.getMessage());
        }
    }

    /**
     * Crea una nueva oferta de trueque.
     *
     * @param creatorUUID UUID del creador
     * @param creatorName Nombre del creador
     * @param offeredItem Item ofrecido
     * @param requestedItem Item solicitado
     * @return UUID de la oferta creada
     */
    public UUID createOffer(UUID creatorUUID, String creatorName, 
                           net.minecraft.world.item.ItemStack offeredItem, 
                           net.minecraft.world.item.ItemStack requestedItem) {
        UUID offerId = UUID.randomUUID();
        TradeOffer offer = new TradeOffer(offerId, creatorUUID, creatorName, offeredItem, requestedItem);
        activeOffers.put(offerId, offer);
        
        LoggerUtil.info("New trade offer created: " + offer);
        saveOffers();
        
        return offerId;
    }

    /**
     * Obtiene una oferta por su ID.
     *
     * @param offerId UUID de la oferta
     * @return TradeOffer si existe, null en caso contrario
     */
    public TradeOffer getOffer(UUID offerId) {
        return activeOffers.get(offerId);
    }

    /**
     * Obtiene una oferta por su ID (método alternativo).
     *
     * @param offerId UUID de la oferta
     * @return TradeOffer si existe, null en caso contrario
     */
    public TradeOffer getOfferById(UUID offerId) {
        TradeOffer offer = activeOffers.get(offerId);
        if (offer != null && !offer.isExpired()) {
            return offer;
        }
        return null;
    }

    /**
     * Elimina una oferta.
     *
     * @param offerId UUID de la oferta a eliminar
     * @return true si se eliminó exitosamente, false si no existía
     */
    public boolean removeOffer(UUID offerId) {
        boolean removed = activeOffers.remove(offerId) != null;
        if (removed) {
            LoggerUtil.info("Trade offer removed: " + offerId);
            saveOffers();
        }
        return removed;
    }

    /**
     * Obtiene todas las ofertas activas (no expiradas).
     *
     * @return Lista de ofertas activas
     */
    public List<TradeOffer> getActiveOffers() {
        return activeOffers.values().stream()
                .filter(offer -> !offer.isExpired())
                .collect(Collectors.toList());
    }

    /**
     * Obtiene todas las ofertas activas (método alternativo con mismo nombre que usa el contenedor).
     *
     * @return Lista de ofertas activas
     */
    public List<TradeOffer> getAllActiveOffers() {
        return getActiveOffers();
    }

    /**
     * Obtiene todas las ofertas de un jugador específico.
     *
     * @param playerUUID UUID del jugador
     * @return Lista de ofertas del jugador
     */
    public List<TradeOffer> getPlayerOffers(UUID playerUUID) {
        return getActiveOffers().stream()
                .filter(offer -> offer.getCreatorUUID().equals(playerUUID))
                .collect(Collectors.toList());
    }

    /**
     * Busca ofertas que requieran un item específico.
     *
     * @param itemName Nombre del item a buscar
     * @return Lista de ofertas que buscan ese item
     */
    public List<TradeOffer> searchOffersByRequest(String itemName) {
        String lowerName = itemName.toLowerCase();
        return getActiveOffers().stream()
                .filter(offer -> offer.getRequestedItem().getHoverName().getString()
                        .toLowerCase().contains(lowerName))
                .collect(Collectors.toList());
    }

    /**
     * Limpia las ofertas expiradas.
     */
    public void cleanExpiredOffers() {
        List<UUID> expiredOffers = activeOffers.values().stream()
                .filter(TradeOffer::isExpired)
                .map(TradeOffer::getOfferId)
                .collect(Collectors.toList());

        expiredOffers.forEach(offerId -> {
            activeOffers.remove(offerId);
            LoggerUtil.info("Expired offer cleaned: " + offerId);
        });

        if (!expiredOffers.isEmpty()) {
            saveOffers();
        }
    }

    /**
     * Guarda todas las ofertas en archivo JSON.
     */
    public void saveOffers() {
        if (dataDirectory == null) {
            LoggerUtil.error("Cannot save offers: data directory not initialized");
            return;
        }
        
        try {
            Path filePath = dataDirectory.resolve(OFFERS_FILE);
            
            // Crear JSON manualmente
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"offers\": [\n");
            
            List<TradeOffer> offersList = new ArrayList<>(activeOffers.values());
            for (int i = 0; i < offersList.size(); i++) {
                TradeOffer offer = offersList.get(i);
                json.append("    {\n");
                json.append("      \"offerId\": \"").append(offer.getOfferId()).append("\",\n");
                json.append("      \"creatorUUID\": \"").append(offer.getCreatorUUID()).append("\",\n");
                json.append("      \"creatorName\": \"").append(offer.getCreatorName()).append("\",\n");
                json.append("      \"offeredItem\": \"").append(offer.getOfferedItem().getItem().toString()).append("\",\n");
                json.append("      \"offeredCount\": ").append(offer.getOfferedItem().getCount()).append(",\n");
                json.append("      \"requestedItem\": \"").append(offer.getRequestedItem().getItem().toString()).append("\",\n");
                json.append("      \"requestedCount\": ").append(offer.getRequestedItem().getCount()).append(",\n");
                json.append("      \"createdTime\": ").append(offer.getCreatedTime()).append("\n");
                json.append("    }");
                if (i < offersList.size() - 1) {
                    json.append(",");
                }
                json.append("\n");
            }
            
            json.append("  ]\n");
            json.append("}\n");
            
            Files.write(filePath, json.toString().getBytes(StandardCharsets.UTF_8));
            LoggerUtil.info("Saved " + activeOffers.size() + " trade offers to " + filePath.toAbsolutePath());
        } catch (IOException e) {
            LoggerUtil.error("Failed to save trade offers: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Carga todas las ofertas desde archivo JSON usando NBT como respaldo.
     */
    public void loadOffers() {
        if (dataDirectory == null) {
            LoggerUtil.info("Data directory not initialized yet, offers will be loaded later");
            return;
        }
        
        try {
            Path jsonPath = dataDirectory.resolve(OFFERS_FILE);
            Path nbtPath = dataDirectory.resolve("trade_offers.nbt");
            
            // Intentar cargar desde JSON primero
            if (Files.exists(jsonPath)) {
                loadFromJson(jsonPath);
                return;
            }
            
            // Si no existe JSON, intentar migrar desde NBT
            if (Files.exists(nbtPath)) {
                LoggerUtil.info("Migrating from NBT format to JSON...");
                loadFromNBT(nbtPath);
                // Guardar en formato JSON
                saveOffers();
                // Eliminar archivo NBT viejo
                Files.delete(nbtPath);
                LoggerUtil.info("Migration completed");
                return;
            }
            
            LoggerUtil.info("No previous trade offers found");
        } catch (Exception e) {
            LoggerUtil.error("Failed to load trade offers: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void loadFromJson(Path filePath) throws IOException {
        String content = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
        
        // Parse JSON manualmente (simple)
        String[] lines = content.split("\n");
        UUID currentOfferId = null;
        UUID currentCreatorUUID = null;
        String currentCreatorName = null;
        String currentOfferedItem = null;
        int currentOfferedCount = 1;
        String currentRequestedItem = null;
        int currentRequestedCount = 1;
        long currentCreatedTime = 0;
        
        for (String line : lines) {
            line = line.trim();
            
            if (line.contains("\"offerId\"")) {
                String value = extractJsonValue(line);
                currentOfferId = UUID.fromString(value);
            } else if (line.contains("\"creatorUUID\"")) {
                String value = extractJsonValue(line);
                currentCreatorUUID = UUID.fromString(value);
            } else if (line.contains("\"creatorName\"")) {
                currentCreatorName = extractJsonValue(line);
            } else if (line.contains("\"offeredItem\"")) {
                currentOfferedItem = extractJsonValue(line);
            } else if (line.contains("\"offeredCount\"")) {
                currentOfferedCount = Integer.parseInt(extractJsonValue(line));
            } else if (line.contains("\"requestedItem\"")) {
                currentRequestedItem = extractJsonValue(line);
            } else if (line.contains("\"requestedCount\"")) {
                currentRequestedCount = Integer.parseInt(extractJsonValue(line));
            } else if (line.contains("\"createdTime\"")) {
                currentCreatedTime = Long.parseLong(extractJsonValue(line));
                
                // Crear oferta cuando tenemos todos los datos
                if (currentOfferId != null && currentCreatorUUID != null && currentOfferedItem != null && currentRequestedItem != null) {
                    try {
                        net.minecraft.world.item.Item offeredItemObj = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation(currentOfferedItem));
                        net.minecraft.world.item.Item requestedItemObj = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation(currentRequestedItem));
                        
                        if (offeredItemObj != null && requestedItemObj != null) {
                            net.minecraft.world.item.ItemStack offeredStack = new net.minecraft.world.item.ItemStack(offeredItemObj, currentOfferedCount);
                            net.minecraft.world.item.ItemStack requestedStack = new net.minecraft.world.item.ItemStack(requestedItemObj, currentRequestedCount);
                            
                            TradeOffer offer = new TradeOffer(currentOfferId, currentCreatorUUID, currentCreatorName, offeredStack, requestedStack, currentCreatedTime);
                            
                            if (!offer.isExpired()) {
                                activeOffers.put(currentOfferId, offer);
                            }
                        }
                    } catch (Exception e) {
                        LoggerUtil.error("Failed to load offer " + currentOfferId + ": " + e.getMessage());
                    }
                    
                    // Reset para la siguiente oferta
                    currentOfferId = null;
                    currentCreatorUUID = null;
                    currentCreatorName = null;
                    currentOfferedItem = null;
                    currentOfferedCount = 1;
                    currentRequestedItem = null;
                    currentRequestedCount = 1;
                    currentCreatedTime = 0;
                }
            }
        }
        
        LoggerUtil.info("Loaded " + activeOffers.size() + " trade offers from JSON");
    }
    
    private String extractJsonValue(String line) {
        // Extrae el valor de "key": "value" o "key": value
        int colonIndex = line.indexOf(":");
        if (colonIndex == -1) return "";
        
        String value = line.substring(colonIndex + 1).trim();
        // Remover comas y comillas
        value = value.replace(",", "").replace("\"", "").trim();
        return value;
    }
    
    private void loadFromNBT(Path filePath) throws IOException {
        byte[] data = Files.readAllBytes(filePath);
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bais);
        CompoundTag rootTag = net.minecraft.nbt.NbtIo.read(dis);
        dis.close();

        if (rootTag != null && rootTag.contains("Offers")) {
            ListTag offersList = rootTag.getList("Offers", Tag.TAG_COMPOUND);
            
            for (int i = 0; i < offersList.size(); i++) {
                CompoundTag offerTag = offersList.getCompound(i);
                TradeOffer offer = TradeOffer.deserializeNBT(offerTag);
                
                // Solo cargar si no ha expirado
                if (!offer.isExpired()) {
                    activeOffers.put(offer.getOfferId(), offer);
                }
            }
            
            LoggerUtil.info("Loaded " + activeOffers.size() + " trade offers from NBT");
        }
    }

    /**
     * Maneja el guardado periódico durante los ticks del servidor.
     *
     * @param event Evento de tick
     */
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        saveCounter++;
        if (saveCounter >= SAVE_INTERVAL) {
            saveCounter = 0;
            cleanExpiredOffers();
            saveOffers();
        }
    }

    /**
     * Obtiene el número total de ofertas activas.
     *
     * @return Cantidad de ofertas
     */
    public int getTotalActiveOffers() {
        return getActiveOffers().size();
    }

    /**
     * Reinicia el manager (útil para recargar datos).
     */
    public void reload() {
        activeOffers.clear();
        loadOffers();
        LoggerUtil.info("Trade offer manager reloaded");
    }
}
