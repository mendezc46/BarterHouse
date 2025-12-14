package com.barterhouse;

import com.barterhouse.commands.CommandRegistry;
import com.barterhouse.manager.TradeOfferManager;
import com.barterhouse.util.LoggerUtil;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Clase principal del mod BarterHouse.
 * Gestiona la inicialización y eventos del servidor.
 */
@Mod("barterhouse")
public class BarterHouseMod {

    public static final String MOD_ID = "barterhouse";
    public static final String MOD_NAME = "BarterHouse";
    public static final String MOD_VERSION = "1.0.0";

    public BarterHouseMod() {
        LoggerUtil.info(MOD_NAME + " " + MOD_VERSION + " iniciando...");
        LoggerUtil.info("BarterHouse mod loaded successfully!");
    }

    /**
     * Evento de configuración común.
     */
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class CommonSetupEvents {

        @SubscribeEvent
        public static void onCommonSetup(FMLCommonSetupEvent event) {
            LoggerUtil.info(MOD_NAME + " common setup complete");
        }
    }

    /**
     * Manejador de eventos del servidor.
     */
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ServerEvents {

        /**
         * Se ejecuta cuando se registran los comandos.
         */
        @SubscribeEvent
        public static void onCommandsRegister(RegisterCommandsEvent event) {
            CommandRegistry.register(event.getDispatcher());
            LoggerUtil.info(MOD_NAME + " commands registered");
        }

        /**
         * Se ejecuta cuando el servidor inicia.
         */
        @SubscribeEvent
        public static void onServerStarting(ServerStartingEvent event) {
            LoggerUtil.info(MOD_NAME + " server starting");
            TradeOfferManager.getInstance().reload();
            LoggerUtil.info("Trade offer manager initialized with " + 
                           TradeOfferManager.getInstance().getTotalActiveOffers() + " offers");
        }

        /**
         * Se ejecuta cuando el servidor se detiene.
         */
        @SubscribeEvent
        public static void onServerStopping(ServerStoppingEvent event) {
            LoggerUtil.info(MOD_NAME + " server stopping");
            TradeOfferManager.getInstance().saveOffers();
            LoggerUtil.info("Trade offers saved successfully");
        }
    }
}
