package com.barterhouse.event;

import com.barterhouse.BarterHouseMod;
import com.barterhouse.commands.BarterUIManager;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Maneja eventos de contenedores - SERVER SIDE ONLY
 */
@Mod.EventBusSubscriber(modid = BarterHouseMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ContainerClickHandler {

    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        Player player = event.getEntity();
        BarterUIManager.removePlayerMenu(player.getUUID());
    }
}
