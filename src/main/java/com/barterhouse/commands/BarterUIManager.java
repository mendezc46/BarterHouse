package com.barterhouse.commands;

import com.barterhouse.api.TradeOffer;
import com.barterhouse.manager.TradeOfferManager;
import com.barterhouse.menu.BarterChestMenu;
import com.barterhouse.util.LoggerUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.HashMap;
import java.util.UUID;

/**
 * Gestor de GUIs usando ChestMenu vanilla (server-side puro)
 */
public class BarterUIManager {

    // Rastreador de qué menú tiene abierto cada jugador
    private static final HashMap<UUID, String> playerMenus = new HashMap<>();

    /**
     * Abre un menú de cofre con la lista de ofertas
     */
    public static void openOffersListGUI(Player player) {
        LoggerUtil.info("openOffersListGUI called for player: " + player.getName().getString());
        
        if (!(player instanceof ServerPlayer)) {
            LoggerUtil.error("Player is not a ServerPlayer!");
            return;
        }
        
        try {
            ServerPlayer serverPlayer = (ServerPlayer) player;
            
            // Crear un contenedor de cofre GRANDE (6 filas = 54 slots)
            SimpleContainer container = new SimpleContainer(54);
            
            // Cargar ofertas
            List<TradeOffer> offers = TradeOfferManager.getInstance().getAllActiveOffers();
            LoggerUtil.info("Loading " + offers.size() + " offers");
            
            // Llenar las primeras 4 filas (slots 0-35) con ofertas
            int slot = 0;
            for (TradeOffer offer : offers) {
                if (slot >= 36) break;
                
                ItemStack displayStack = offer.getOfferedItem().copy();
                displayStack.setCount(1);
                container.setItem(slot, displayStack);
                slot++;
            }
            
            // Fila 6 (slots 45-53): Botones de acción
            ItemStack createButton = new ItemStack(Items.EMERALD);
            createButton.setHoverName(Component.literal("§a§lCrear Oferta"));
            container.setItem(45, createButton);
            
            ItemStack deleteButton = new ItemStack(Items.REDSTONE);
            deleteButton.setHoverName(Component.literal("§c§lBorrar Mis Ofertas"));
            container.setItem(46, deleteButton);
            
            ItemStack warehouseButton = new ItemStack(Items.CHEST);
            warehouseButton.setHoverName(Component.literal("§6§lBodega"));
            container.setItem(47, warehouseButton);
            
            // Marcar que este jugador tiene el menú de ofertas abierto
            playerMenus.put(player.getUUID(), "offers");
            
            // Abrir menú con BarterChestMenu personalizado
            serverPlayer.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (windowId, playerInventory, playerEntity) -> 
                    new BarterChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x6, windowId, playerInventory, container, 6, serverPlayer, "offers"),
                Component.literal("BarterHouse - Ofertas (" + offers.size() + ")")
            ));
            
            LoggerUtil.info("Chest menu opened successfully!");
        } catch (Exception e) {
            LoggerUtil.error("Error opening OffersListGUI: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Abre un menú para crear oferta
     * Layout: 3 filas (27 slots)
     * - Centro (slot 13): Donde poner el item a tradear
     * - Alrededor del centro: Vidrio (slots 3,4,5,12,14,21,22,23)
     * - Slot 18: Letrero para buscar item solicitado
     */
    public static void openCreateOfferGUI(Player player) {
        LoggerUtil.info("openCreateOfferGUI called for player: " + player.getName().getString());
        
        if (!(player instanceof ServerPlayer)) {
            return;
        }
        
        try {
            ServerPlayer serverPlayer = (ServerPlayer) player;
            
            // 3 filas = 27 slots
            SimpleContainer container = new SimpleContainer(27);
            
            // Patrón 3x3 con vidrio alrededor del slot central (13)
            // Fila 1: slots 3,4,5
            container.setItem(3, new ItemStack(Items.GLASS_PANE));
            container.setItem(4, new ItemStack(Items.GLASS_PANE));
            container.setItem(5, new ItemStack(Items.GLASS_PANE));
            
            // Fila 2: slots 12, [13 VACÍO], 14
            container.setItem(12, new ItemStack(Items.GLASS_PANE));
            
            // Slot 13 = centro, restaurar el item guardado si existe
            ItemStack savedItem = com.barterhouse.event.SignEditHandler.getOfferedItem(player.getUUID());
            if (savedItem != null && !savedItem.isEmpty()) {
                container.setItem(13, savedItem.copy());
                LoggerUtil.info("Restored offered item to slot 13 for player: " + player.getName().getString());
            }
            
            container.setItem(14, new ItemStack(Items.GLASS_PANE));
            
            // Fila 3: slots 21,22,23
            container.setItem(21, new ItemStack(Items.GLASS_PANE));
            container.setItem(22, new ItemStack(Items.GLASS_PANE));
            container.setItem(23, new ItemStack(Items.GLASS_PANE));
            
            // Letrero para buscar item solicitado (slot 18 - esquina inferior izquierda)
            ItemStack signItem = new ItemStack(Items.OAK_SIGN);
            signItem.setHoverName(Component.literal("§e§lBuscar Item Solicitado"));
            container.setItem(18, signItem);
            
            playerMenus.put(player.getUUID(), "create");
            
            serverPlayer.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (windowId, playerInventory, playerEntity) -> 
                    new BarterChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x3, windowId, playerInventory, container, 3, serverPlayer, "create"),
                Component.literal("BarterHouse - Crear Oferta")
            ));
            
            LoggerUtil.info("Create offer menu opened!");
        } catch (Exception e) {
            LoggerUtil.error("Error opening CreateOfferGUI: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void openSearchItemGUI(Player player) {
        player.displayClientMessage(Component.literal("§e[BarterHouse] Usa /barter list para ver ofertas"), false);
    }

    public static void openConfirmOfferGUI(Player player) {
        player.displayClientMessage(Component.literal("§e[BarterHouse] Usa /barter accept <id> para aceptar"), false);
    }
    
    public static void openDeleteMyOffersGUI(Player player) {
        LoggerUtil.info("openDeleteMyOffersGUI called for player: " + player.getName().getString());
        
        if (!(player instanceof ServerPlayer)) {
            return;
        }
        
        try {
            ServerPlayer serverPlayer = (ServerPlayer) player;
            
            SimpleContainer container = new SimpleContainer(54);
            
            List<TradeOffer> myOffers = TradeOfferManager.getInstance().getAllActiveOffers()
                .stream()
                .filter(offer -> offer.getCreatorUUID().equals(player.getUUID()))
                .collect(java.util.stream.Collectors.toList());
            
            LoggerUtil.info("Player has " + myOffers.size() + " offers");
            
            int slot = 0;
            for (TradeOffer offer : myOffers) {
                if (slot >= 36) break;
                
                ItemStack displayStack = offer.getOfferedItem().copy();
                displayStack.setCount(1);
                container.setItem(slot, displayStack);
                slot++;
            }
            
            ItemStack backButton = new ItemStack(Items.ARROW);
            backButton.setHoverName(Component.literal("§e§lVolver"));
            container.setItem(45, backButton);
            
            playerMenus.put(player.getUUID(), "delete");
            
            serverPlayer.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (windowId, playerInventory, playerEntity) -> 
                    new BarterChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x6, windowId, playerInventory, container, 6, serverPlayer, "delete"),
                Component.literal("Mis Ofertas (" + myOffers.size() + ")")
            ));
            
            LoggerUtil.info("Delete my offers menu opened!");
        } catch (Exception e) {
            LoggerUtil.error("Error opening DeleteMyOffersGUI: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static String getPlayerMenu(UUID playerUUID) {
        return playerMenus.getOrDefault(playerUUID, "");
    }
    
    /**
     * Abre la bodega del jugador (almacén de items recibidos)
     */
    public static void openWarehouseGUI(Player player) {
        LoggerUtil.info("openWarehouseGUI called for player: " + player.getName().getString());
        
        if (!(player instanceof ServerPlayer)) {
            return;
        }
        
        try {
            ServerPlayer serverPlayer = (ServerPlayer) player;
            
            // Crear un contenedor de cofre GRANDE (6 filas = 54 slots)
            SimpleContainer container = new SimpleContainer(54);
            
            // Cargar items de la bodega
            java.util.List<com.barterhouse.manager.WarehouseManager.StoredItem> warehouseItems = 
                com.barterhouse.manager.WarehouseManager.getInstance().getPlayerWarehouse(player.getUUID());
            
            LoggerUtil.info("Loading " + warehouseItems.size() + " items from warehouse");
            
            // Llenar la bodega con los items (primeras 4 filas = slots 0-35)
            int slot = 0;
            for (com.barterhouse.manager.WarehouseManager.StoredItem storedItem : warehouseItems) {
                if (slot >= 36) break;
                
                try {
                    // Crear un ItemStack desde el nombre registrado
                    net.minecraft.world.item.Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS
                        .getValue(new net.minecraft.resources.ResourceLocation(storedItem.itemName));
                    
                    if (item != null) {
                        ItemStack displayStack = new ItemStack(item, Math.min(storedItem.count, 64));
                        
                        // Mostrar información del item
                        String displayName = "§e" + storedItem.count + "x §7" + item.getDescription().getString() +
                                           "\n§7De: §6" + storedItem.sourcePlayer;
                        displayStack.setHoverName(net.minecraft.network.chat.Component.literal(displayName));
                        
                        container.setItem(slot, displayStack);
                        slot++;
                    }
                } catch (Exception e) {
                    LoggerUtil.error("Error loading warehouse item: " + storedItem.itemName);
                }
            }
            
            // Fila 6 (slot 45): Botón Volver (Flecha)
            ItemStack backButton = new ItemStack(Items.ARROW);
            backButton.setHoverName(net.minecraft.network.chat.Component.literal("§e§lVOLVER"));
            container.setItem(45, backButton);
            
            // Marcar que este jugador tiene el menú de bodega abierto
            playerMenus.put(player.getUUID(), "warehouse");
            
            // Abrir menú con BarterChestMenu personalizado
            serverPlayer.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (windowId, playerInventory, playerEntity) -> 
                    new BarterChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x6, windowId, playerInventory, container, 6, serverPlayer, "warehouse"),
                net.minecraft.network.chat.Component.literal("§7Bodega (" + warehouseItems.size() + " tipos de items)")
            ));
            
            LoggerUtil.info("Warehouse menu opened successfully!");
        } catch (Exception e) {
            LoggerUtil.error("Error opening WarehouseGUI: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void putPlayerMenu(UUID playerUUID, String menuType) {
        playerMenus.put(playerUUID, menuType);
    }
    
    public static void removePlayerMenu(UUID playerUUID) {
        playerMenus.remove(playerUUID);
    }
}

