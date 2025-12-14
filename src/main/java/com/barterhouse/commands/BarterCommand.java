package com.barterhouse.commands;

import com.barterhouse.api.TradeOffer;
import com.barterhouse.event.SignInteractionHandler;
import com.barterhouse.handler.TradeItemHandler;
import com.barterhouse.manager.TradeOfferManager;
import com.barterhouse.util.LoggerUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * Comando principal /barter para abrir GUI y gestionar ofertas
 */
public class BarterCommand {

    public static void execute(CommandSourceStack source, String[] args) {
        LoggerUtil.info("=== /barter command executed ===");
        
        if (!(source.getEntity() instanceof Player)) {
            LoggerUtil.warn("Solo jugadores pueden usar /barter");
            source.sendFailure(Component.literal("§cSolo jugadores pueden usar /barter"));
            return;
        }

        Player player = (Player) source.getEntity();
        LoggerUtil.info("Player: " + player.getName().getString() + " executed /barter");

        // Sin argumentos: mostrar lista de ofertas
        if (args.length == 0) {
            LoggerUtil.info("Opening OffersListGUI for " + player.getName().getString());
            player.displayClientMessage(Component.literal("§aAbriendo lista de ofertas..."), false);
            BarterUIManager.openOffersListGUI(player);
            return;
        }

        String subcommand = args[0].toLowerCase();
        LoggerUtil.info("Subcommand: " + subcommand);

        switch (subcommand) {
            case "create":
                LoggerUtil.info("Executing: create");
                handleCreate(player);
                break;
            case "list":
                LoggerUtil.info("Executing: list");
                BarterUIManager.openOffersListGUI(player);
                player.displayClientMessage(Component.literal("§aAbriendo lista de ofertas..."), false);
                break;
            case "accept":
                if (args.length < 2) {
                    player.displayClientMessage(Component.literal("§cUso: /barter accept <id>"), false);
                    return;
                }
                LoggerUtil.info("Executing: accept " + args[1]);
                handleAccept(player, args[1]);
                break;
            case "cancel":
                if (args.length < 2) {
                    player.displayClientMessage(Component.literal("§cUso: /barter cancel <id>"), false);
                    return;
                }
                LoggerUtil.info("Executing: cancel " + args[1]);
                handleCancel(player, args[1]);
                break;
            default:
                player.displayClientMessage(Component.literal("§cComando desconocido. Usa: /barter, /barter create, /barter list"), false);
        }
    }

    /**
     * Abre el flow de creación de oferta.
     */
    private static void handleCreate(Player player) {
        BarterUIManager.openCreateOfferGUI(player);
        player.displayClientMessage(Component.literal("§b[BarterHouse] §aSelecciona el item que deseas ofrecer"), false);
    }

    /**
     * Maneja la aceptación de una oferta existente.
     * Ejecuta la transacción si el jugador tiene el item solicitado.
     */
    private static void handleAccept(Player player, String offerId) {
        try {
            UUID offerUUID = UUID.fromString(offerId);
            TradeOfferManager manager = TradeOfferManager.getInstance();
            TradeOffer offer = manager.getOfferById(offerUUID);

            if (offer == null) {
                player.displayClientMessage(Component.literal("§cOferta no encontrada"), false);
                return;
            }

            // Verificar que no sea el creador de la oferta
            if (offer.getCreatorUUID().equals(player.getUUID())) {
                player.displayClientMessage(Component.literal("§cNo puedes aceptar tu propia oferta"), false);
                return;
            }

            // Usar SignInteractionHandler para ejecutar la transacción
            SignInteractionHandler.executeTrade(player, offer.getOfferId());

        } catch (IllegalArgumentException e) {
            player.displayClientMessage(Component.literal("§cID de oferta inválido"), false);
        } catch (Exception e) {
            LoggerUtil.error("Error al aceptar oferta: " + e.getMessage());
            player.displayClientMessage(Component.literal("§cError al aceptar la oferta"), false);
        }
    }

    /**
     * Cancela una oferta creada por el jugador.
     */
    private static void handleCancel(Player player, String offerId) {
        try {
            UUID offerUUID = UUID.fromString(offerId);
            TradeOfferManager manager = TradeOfferManager.getInstance();
            TradeOffer offer = manager.getOfferById(offerUUID);

            if (offer == null) {
                player.displayClientMessage(Component.literal("§cOferta no encontrada"), false);
                return;
            }

            // Verificar que sea el creador
            if (!offer.getCreatorUUID().equals(player.getUUID())) {
                player.displayClientMessage(Component.literal("§cSolo el creador puede cancelar esta oferta"), false);
                return;
            }

            // Remover la oferta
            manager.removeOffer(offerUUID);
            manager.saveOffers();

            player.displayClientMessage(Component.literal("§a✓ Oferta cancelada exitosamente"), false);
            LoggerUtil.info("Oferta cancelada por " + player.getName().getString() + ": " + offerId);

        } catch (IllegalArgumentException e) {
            player.displayClientMessage(Component.literal("§cID de oferta inválido"), false);
        } catch (Exception e) {
            LoggerUtil.error("Error al cancelar oferta: " + e.getMessage());
            player.displayClientMessage(Component.literal("§cError al cancelar la oferta"), false);
        }
    }
}
