package com.barterhouse.event;

import com.barterhouse.handler.TradeItemHandler;
import com.barterhouse.manager.TradeOfferManager;
import com.barterhouse.util.LoggerUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/**
 * Manejador de interacciones con letreros de trueque.
 * Gestiona la creación de ofertas cuando un jugador toca un letrero.
 */
public class SignInteractionHandler {

    // Clave NBT para almacenar en el jugador qué oferta está viendo
    private static final String CURRENT_SEARCH_KEY = "BarterHouse_CurrentSearch";

    /**
     * Maneja la interacción cuando un jugador toca un letrero.
     * Abre el menú de búsqueda de items para crear una oferta.
     *
     * @param player Jugador que interactúa
     */
    public static void handleSignInteraction(Player player) {
        if (player.level.isClientSide) {
            return;
        }

        // Obtener el item en la mano
        ItemStack mainHandItem = TradeItemHandler.getInstance().getMainHandItem(player);

        // Validar que el jugador tenga un item válido en la mano
        if (!TradeItemHandler.getInstance().isValidTradeItem(mainHandItem)) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§cDebes sostener un item válido para realizar un trueque"),
                    true
            );
            LoggerUtil.info(player.getName().getString() + " intentó interactuar sin un item válido");
            return;
        }

        // Enviar mensaje al jugador con instrucciones
        player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§6[BarterHouse] Escribe el nombre del item que deseas recibir"),
                true
        );

        LoggerUtil.info("Sign interaction handled for: " + player.getName().getString() + 
                       " with item: " + mainHandItem.getHoverName().getString());

        // Aquí iría la apertura del GUI de búsqueda
        // Por ahora simplemente notificamos al jugador
    }

    /**
     * Valida y crea una oferta de trueque.
     *
     * @param player Jugador que crea la oferta
     * @param offeredItem Item que ofrece (debe estar en su mano)
     * @param requestedItemName Nombre del item que solicita
     * @return UUID de la oferta creada, o null si falla
     */
    public static UUID createTradeOffer(Player player, ItemStack offeredItem, String requestedItemName) {
        // Validar que el item esté en la mano
        if (!TradeItemHandler.getInstance().getMainHandItem(player).equals(offeredItem)) {
            LoggerUtil.warn("Trade offer validation failed: Item in hand doesn't match");
            return null;
        }

        // Validar que el item sea válido
        if (!TradeItemHandler.getInstance().isValidTradeItem(offeredItem)) {
            LoggerUtil.warn("Trade offer validation failed: Invalid trade item");
            return null;
        }

        // TODO: Buscar el item solicitado en el registro de items del juego
        // Por ahora usamos un placeholder
        ItemStack requestedItem = new ItemStack(net.minecraft.world.item.Items.DIAMOND);

        // Crear la oferta
        UUID offerId = TradeOfferManager.getInstance().createOffer(
                player.getUUID(),
                player.getName().getString(),
                offeredItem.copy(),
                requestedItem
        );

        player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§a[BarterHouse] Oferta creada con éxito!"),
                true
        );

        LoggerUtil.info("Trade offer created: " + offerId + " by " + player.getName().getString());
        return offerId;
    }

    /**
     * Realiza un trueque entre dos jugadores.
     *
     * @param buyer Jugador que acepta la oferta
     * @param offerId UUID de la oferta
     * @return true si el trueque fue exitoso, false en caso contrario
     */
    public static boolean executeTrade(Player buyer, UUID offerId) {
        // Obtener la oferta
        com.barterhouse.api.TradeOffer offer = TradeOfferManager.getInstance().getOffer(offerId);
        if (offer == null) {
            LoggerUtil.warn("Trade execution failed: Offer not found - " + offerId);
            return false;
        }

        // Obtener el jugador creador de la oferta
        Player creator = buyer.getServer().getPlayerList().getPlayer(offer.getCreatorUUID());
        if (creator == null || creator.isDeadOrDying()) {
            LoggerUtil.warn("Trade execution failed: Creator not found or offline");
            TradeOfferManager.getInstance().removeOffer(offerId);
            return false;
        }

        // Validar que el comprador tenga el item requerido
        if (!TradeItemHandler.getInstance().hasItem(buyer, offer.getRequestedItem())) {
            LoggerUtil.warn("Trade validation failed: Buyer doesn't have required item");
            buyer.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§cNo tienes el item requerido para este trueque"),
                    true
            );
            return false;
        }

        // Validar que el creador aún tenga el item ofrecido
        if (!TradeItemHandler.getInstance().hasItem(creator, offer.getOfferedItem())) {
            LoggerUtil.warn("Trade validation failed: Creator doesn't have offered item anymore");
            creator.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§cAlguien intentó completar tu oferta pero ya no tienes el item"),
                    true
            );
            buyer.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§cEl creador ya no tiene el item disponible"),
                    true
            );
            TradeOfferManager.getInstance().removeOffer(offerId);
            return false;
        }

        // Realizar las transacciones de items
        try {
            // Remover item del comprador
            if (!TradeItemHandler.getInstance().removeItem(buyer, offer.getRequestedItem())) {
                LoggerUtil.warn("Failed to remove item from buyer");
                return false;
            }

            // Remover item del creador
            if (!TradeItemHandler.getInstance().removeItem(creator, offer.getOfferedItem())) {
                LoggerUtil.warn("Failed to remove item from creator, attempting to restore buyer item");
                // Intentar restaurar el item del comprador
                TradeItemHandler.getInstance().addItem(buyer, offer.getRequestedItem());
                return false;
            }

            // Agregar item al comprador
            if (!TradeItemHandler.getInstance().addItem(buyer, offer.getOfferedItem())) {
                LoggerUtil.warn("Failed to add item to buyer, attempting to restore items");
                // Intentar restaurar ambos items
                TradeItemHandler.getInstance().addItem(buyer, offer.getRequestedItem());
                TradeItemHandler.getInstance().addItem(creator, offer.getOfferedItem());
                return false;
            }

            // Agregar item al creador
            if (!TradeItemHandler.getInstance().addItem(creator, offer.getRequestedItem())) {
                LoggerUtil.warn("Failed to add item to creator, attempting to restore items");
                // Intentar restaurar todos los items
                TradeItemHandler.getInstance().addItem(buyer, offer.getOfferedItem().copy());
                TradeItemHandler.getInstance().removeItem(buyer, offer.getOfferedItem());
                TradeItemHandler.getInstance().addItem(creator, offer.getOfferedItem());
                return false;
            }

            // Remover la oferta
            TradeOfferManager.getInstance().removeOffer(offerId);

            // Notificar a ambos jugadores
            buyer.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§a[BarterHouse] ¡Trueque completado con éxito!"),
                    true
            );
            creator.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§a[BarterHouse] ¡Tu oferta fue aceptada!"),
                    true
            );

            LoggerUtil.info("Trade executed successfully: " + offerId + " by " + buyer.getName().getString());
            return true;

        } catch (Exception e) {
            LoggerUtil.error("Unexpected error during trade execution: " + e.getMessage());
            return false;
        }
    }

    /**
     * Cancela una oferta de trueque.
     *
     * @param player Jugador propietario de la oferta
     * @param offerId UUID de la oferta
     * @return true si se canceló exitosamente, false en caso contrario
     */
    public static boolean cancelOffer(Player player, UUID offerId) {
        com.barterhouse.api.TradeOffer offer = TradeOfferManager.getInstance().getOffer(offerId);
        if (offer == null) {
            return false;
        }

        // Verificar que el jugador sea el propietario
        if (!offer.getCreatorUUID().equals(player.getUUID())) {
            LoggerUtil.warn(player.getName().getString() + " tried to cancel someone else's offer");
            return false;
        }

        TradeOfferManager.getInstance().removeOffer(offerId);
        player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§a[BarterHouse] Oferta cancelada"),
                true
        );

        LoggerUtil.info("Offer canceled: " + offerId + " by " + player.getName().getString());
        return true;
    }
}
