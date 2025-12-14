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
    private final Path dataDirectory;
    private static final String OFFERS_FILE = "trade_offers.nbt";

    private static final int SAVE_INTERVAL = 20 * 60; // Guardar cada 60 segundos (20 ticks * 60)
    private int saveCounter = 0;

    /**
     * Constructor privado para el patrón Singleton.
     */
    private TradeOfferManager() {
        this.activeOffers = new HashMap<>();
        this.dataDirectory = Paths.get("world_data", "barterhouse");
        initializeDataDirectory();
        loadOffers();
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
     * Inicializa el directorio de datos si no existe.
     */
    private void initializeDataDirectory() {
        try {
            Files.createDirectories(dataDirectory);
            LoggerUtil.info("Data directory initialized at: " + dataDirectory);
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
     * Guarda todas las ofertas en archivo NBT.
     */
    public void saveOffers() {
        try {
            CompoundTag rootTag = new CompoundTag();
            ListTag offersList = new ListTag();

            for (TradeOffer offer : activeOffers.values()) {
                offersList.add(offer.serializeNBT());
            }

            rootTag.put("Offers", offersList);

            Path filePath = dataDirectory.resolve(OFFERS_FILE);
            byte[] data = new byte[0];
            
            // Serializar CompoundTag a bytes
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            net.minecraft.nbt.NbtIo.write(rootTag, dos);
            dos.close();
            data = baos.toByteArray();

            Files.write(filePath, data);
            LoggerUtil.debug("Saved " + activeOffers.size() + " trade offers");
        } catch (IOException e) {
            LoggerUtil.error("Failed to save trade offers: " + e.getMessage());
        }
    }

    /**
     * Carga todas las ofertas desde archivo NBT.
     */
    public void loadOffers() {
        try {
            Path filePath = dataDirectory.resolve(OFFERS_FILE);
            if (!Files.exists(filePath)) {
                LoggerUtil.info("No previous trade offers found");
                return;
            }

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
                
                LoggerUtil.info("Loaded " + activeOffers.size() + " trade offers");
            }
        } catch (IOException e) {
            LoggerUtil.error("Failed to load trade offers: " + e.getMessage());
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
