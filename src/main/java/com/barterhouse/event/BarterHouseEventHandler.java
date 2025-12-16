package com.barterhouse.event;

import com.barterhouse.handler.TradeItemHandler;
import com.barterhouse.manager.TradeOfferManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
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
     * Maneja los ticks del servidor para limpiar ofertas expiradas.
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        TradeOfferManager.getInstance().onServerTick(event);
    }
}
