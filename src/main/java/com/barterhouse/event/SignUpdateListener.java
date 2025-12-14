package com.barterhouse.event;

import com.barterhouse.BarterHouseMod;
import com.barterhouse.util.LoggerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.UUID;

/**
 * Captura eventos de actualización de letreros - SERVER SIDE ONLY
 */
@Mod.EventBusSubscriber(modid = BarterHouseMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SignUpdateListener {
    
    // Rastrear cuándo verificar letreros actualizados
    private static final HashMap<UUID, Integer> ticksToCheck = new HashMap<>();
    
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer)) return;
        
        ServerPlayer player = (ServerPlayer) event.player;
        UUID playerUUID = player.getUUID();
        
        // Verificar si este jugador tiene un letrero de búsqueda pendiente
        BlockPos signPos = SignEditHandler.getSearchingSign(playerUUID);
        if (signPos == null) return;
        
        // Incrementar contador de ticks
        int ticks = ticksToCheck.getOrDefault(playerUUID, 0) + 1;
        ticksToCheck.put(playerUUID, ticks);
        
        // Cada 20 ticks (1 segundo) verificar si el letrero fue actualizado
        if (ticks % 20 == 0) {
            BlockEntity be = player.level.getBlockEntity(signPos);
            if (be instanceof SignBlockEntity) {
                SignBlockEntity sign = (SignBlockEntity) be;
                
                // Obtener el texto del letrero (en 1.19.2 está en messages)
                String[] lines = new String[4];
                for (int i = 0; i < 4; i++) {
                    lines[i] = sign.getMessage(i, false).getString();
                }
                
                // Verificar si hay texto
                boolean hasText = false;
                for (String line : lines) {
                    if (line != null && !line.isEmpty()) {
                        hasText = true;
                        break;
                    }
                }
                
                // Si hay texto, procesar la búsqueda
                if (hasText) {
                    ticksToCheck.remove(playerUUID);
                    SignEditHandler.processSignText(player, signPos, lines);
                }
            }
        }
        
        // Si pasaron más de 200 ticks (10 segundos) sin actualizar, cancelar
        if (ticks > 200) {
            ticksToCheck.remove(playerUUID);
            SignEditHandler.clearSearchingSign(playerUUID);
            player.level.removeBlock(signPos, false);
            LoggerUtil.info("Sign search timed out for player " + player.getName().getString());
        }
    }
}
