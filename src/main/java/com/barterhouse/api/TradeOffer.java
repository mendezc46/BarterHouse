package com.barterhouse.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * Clase que representa una oferta de trueque en el sistema BarterHouse.
 * Contiene la información del jugador que ofrece, el ítem ofrecido y el ítem buscado.
 */
public class TradeOffer {

    private final UUID offerId;
    private final UUID creatorUUID;
    private final String creatorName;
    private final ItemStack offeredItem;
    private final ItemStack requestedItem;
    private final long creationTime;
    private static final long EXPIRATION_TIME = 7 * 24 * 60 * 60 * 1000; // 7 días en milisegundos

    /**
     * Constructor para crear una nueva oferta de trueque.
     *
     * @param offerId UUID único de la oferta
     * @param creatorUUID UUID del jugador que crea la oferta
     * @param creatorName Nombre del jugador que crea la oferta
     * @param offeredItem Item que se ofrece
     * @param requestedItem Item que se solicita
     */
    public TradeOffer(UUID offerId, UUID creatorUUID, String creatorName, ItemStack offeredItem, ItemStack requestedItem) {
        this.offerId = offerId;
        this.creatorUUID = creatorUUID;
        this.creatorName = creatorName;
        this.offeredItem = offeredItem.copy();
        this.requestedItem = requestedItem.copy();
        this.creationTime = System.currentTimeMillis();
    }
    
    /**
     * Constructor para cargar una oferta existente con tiempo de creación específico.
     *
     * @param offerId UUID único de la oferta
     * @param creatorUUID UUID del jugador que crea la oferta
     * @param creatorName Nombre del jugador que crea la oferta
     * @param offeredItem Item que se ofrece
     * @param requestedItem Item que se solicita
     * @param creationTime Tiempo de creación en milisegundos
     */
    public TradeOffer(UUID offerId, UUID creatorUUID, String creatorName, ItemStack offeredItem, ItemStack requestedItem, long creationTime) {
        this.offerId = offerId;
        this.creatorUUID = creatorUUID;
        this.creatorName = creatorName;
        this.offeredItem = offeredItem.copy();
        this.requestedItem = requestedItem.copy();
        this.creationTime = creationTime;
    }

    // Getters
    public UUID getOfferId() {
        return offerId;
    }

    public UUID getCreatorUUID() {
        return creatorUUID;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public ItemStack getOfferedItem() {
        return offeredItem.copy();
    }

    public ItemStack getRequestedItem() {
        return requestedItem.copy();
    }

    public long getCreationTime() {
        return creationTime;
    }
    
    public long getCreatedTime() {
        return creationTime;
    }

    /**
     * Verifica si la oferta ha expirado.
     *
     * @return true si la oferta ha expirado, false en caso contrario
     */
    public boolean isExpired() {
        return System.currentTimeMillis() - creationTime > EXPIRATION_TIME;
    }

    /**
     * Obtiene el tiempo restante en milisegundos antes de que expire la oferta.
     *
     * @return Tiempo en milisegundos
     */
    public long getTimeRemaining() {
        long remaining = EXPIRATION_TIME - (System.currentTimeMillis() - creationTime);
        return Math.max(0, remaining);
    }

    /**
     * Serializa la oferta a NBT para almacenarla.
     *
     * @return CompoundTag con los datos de la oferta
     */
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("OfferId", offerId.toString());
        tag.putString("CreatorUUID", creatorUUID.toString());
        tag.putString("CreatorName", creatorName);
        tag.put("OfferedItem", offeredItem.serializeNBT());
        tag.put("RequestedItem", requestedItem.serializeNBT());
        tag.putLong("CreationTime", creationTime);
        return tag;
    }

    /**
     * Deserializa una oferta desde NBT.
     *
     * @param tag CompoundTag con los datos
     * @return TradeOffer reconstruida
     */
    public static TradeOffer deserializeNBT(CompoundTag tag) {
        UUID offerId = UUID.fromString(tag.getString("OfferId"));
        UUID creatorUUID = UUID.fromString(tag.getString("CreatorUUID"));
        String creatorName = tag.getString("CreatorName");
        ItemStack offeredItem = ItemStack.of(tag.getCompound("OfferedItem"));
        ItemStack requestedItem = ItemStack.of(tag.getCompound("RequestedItem"));

        TradeOffer offer = new TradeOffer(offerId, creatorUUID, creatorName, offeredItem, requestedItem);
        return offer;
    }

    @Override
    public String toString() {
        return "TradeOffer{" +
                "offerId=" + offerId +
                ", creatorName='" + creatorName + '\'' +
                ", offeredItem=" + offeredItem.getHoverName().getString() +
                ", requestedItem=" + requestedItem.getHoverName().getString() +
                ", expired=" + isExpired() +
                '}';
    }
}
