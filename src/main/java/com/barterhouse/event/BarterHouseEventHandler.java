package com.barterhouse.event;

import com.barterhouse.handler.TradeItemHandler;
import com.barterhouse.manager.TradeOfferManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;

/**
 * Manejador de eventos principales del mod BarterHouse.
 * Gestiona las interacciones con letreros y los ticks del servidor.
 */
@Mod.EventBusSubscriber(modid = "barterhouse", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BarterHouseEventHandler {

    /**
     * Maneja el evento de interacción con bloques (letreros).
     * Se ejecuta cuando el jugador hace clic derecho en un letrero.
     */
    @SubscribeEvent
    public static void onPlayerInteractBlock(PlayerInteractEvent.RightClickBlock event) {
        // Verificar que sea un letrero
        if (event.getLevel().getBlockState(event.getPos()).getBlock() != Blocks.OAK_SIGN &&
            event.getLevel().getBlockState(event.getPos()).getBlock() != Blocks.OAK_WALL_SIGN &&
            event.getLevel().getBlockState(event.getPos()).getBlock() != Blocks.SPRUCE_SIGN &&
            event.getLevel().getBlockState(event.getPos()).getBlock() != Blocks.SPRUCE_WALL_SIGN &&
            event.getLevel().getBlockState(event.getPos()).getBlock() != Blocks.BIRCH_SIGN &&
            event.getLevel().getBlockState(event.getPos()).getBlock() != Blocks.BIRCH_WALL_SIGN &&
            event.getLevel().getBlockState(event.getPos()).getBlock() != Blocks.JUNGLE_SIGN &&
            event.getLevel().getBlockState(event.getPos()).getBlock() != Blocks.JUNGLE_WALL_SIGN &&
            event.getLevel().getBlockState(event.getPos()).getBlock() != Blocks.ACACIA_SIGN &&
            event.getLevel().getBlockState(event.getPos()).getBlock() != Blocks.ACACIA_WALL_SIGN &&
            event.getLevel().getBlockState(event.getPos()).getBlock() != Blocks.DARK_OAK_SIGN &&
            event.getLevel().getBlockState(event.getPos()).getBlock() != Blocks.DARK_OAK_WALL_SIGN &&
            event.getLevel().getBlockState(event.getPos()).getBlock() != Blocks.MANGROVE_SIGN &&
            event.getLevel().getBlockState(event.getPos()).getBlock() != Blocks.MANGROVE_WALL_SIGN) {
            return;
        }

        if (event.getEntity() == null || event.getEntity().level.isClientSide) {
            return;
        }

        // Llamar al handler de interacción con letrero
        SignInteractionHandler.handleSignInteraction((Player) event.getEntity());
        event.setCanceled(true);
    }

    /**
     * Maneja los ticks del servidor para limpiar ofertas expiradas.
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        TradeOfferManager.getInstance().onServerTick(event);
    }
}
