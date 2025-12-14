package com.barterhouse.event;

import com.barterhouse.BarterHouseMod;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Rastrea jugadores que se están desconectando
 */
@Mod.EventBusSubscriber(modid = BarterHouseMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerDisconnectTracker {
    
    private static final Set<UUID> disconnectingPlayers = new HashSet<>();
    
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        // Marcar al jugador como desconectándose ANTES de que se cierre el menú
        disconnectingPlayers.add(event.getEntity().getUUID());
    }
    
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        // Limpiar el flag cuando vuelve a conectarse
        disconnectingPlayers.remove(event.getEntity().getUUID());
    }
    
    public static boolean isDisconnecting(UUID playerUUID) {
        return disconnectingPlayers.contains(playerUUID);
    }
}
