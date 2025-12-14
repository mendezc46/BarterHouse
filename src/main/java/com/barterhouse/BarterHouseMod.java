package com.barterhouse;

import com.barterhouse.commands.CommandRegistry;
import com.barterhouse.config.MessageConfig;
import com.barterhouse.manager.TradeOfferManager;
import com.barterhouse.manager.WarehouseManager;
import com.barterhouse.util.LoggerUtil;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
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
        }
        
        /**
         * Se ejecuta cuando el servidor ha iniciado completamente.
         */
        @SubscribeEvent
        public static void onServerStarted(ServerStartedEvent event) {
            LoggerUtil.info(MOD_NAME + " server started, initializing managers");
            
            // Obtener el overworld para inicializar los directorios de datos
            net.minecraft.server.level.ServerLevel overworld = event.getServer().getLevel(net.minecraft.world.level.Level.OVERWORLD);
            
            if (overworld != null) {
                // Inicializar MessageConfig
                MessageConfig.getInstance().initializeWithLevel(overworld);
                
                // Inicializar TradeOfferManager
                TradeOfferManager.getInstance().initializeWithLevel(overworld);
                LoggerUtil.info("Trade offer manager initialized with " + 
                               TradeOfferManager.getInstance().getTotalActiveOffers() + " offers");
                
                // Inicializar WarehouseManager
                WarehouseManager.getInstance().initializeWithLevel(overworld);
                LoggerUtil.info("Warehouse manager initialized");
            } else {
                LoggerUtil.error("Failed to get overworld level for manager initialization");
            }
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
