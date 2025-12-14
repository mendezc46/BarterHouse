package com.barterhouse.event;

import com.barterhouse.BarterHouseMod;
import com.barterhouse.commands.BarterUIManager;
import com.barterhouse.util.LoggerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Maneja la edición de letreros para buscar items - SERVER SIDE ONLY
 */
@Mod.EventBusSubscriber(modid = BarterHouseMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SignEditHandler {
    
    // Almacena qué jugadores están editando letreros de búsqueda
    private static final HashMap<UUID, BlockPos> searchingSigns = new HashMap<>();
    
    // Almacena el item que el jugador está ofreciendo (el del slot central)
    private static final HashMap<UUID, ItemStack> offeredItems = new HashMap<>();
    
    public static void setOfferedItem(UUID playerUUID, ItemStack item) {
        offeredItems.put(playerUUID, item.copy());
        LoggerUtil.info("Stored offered item for player " + playerUUID + ": " + item);
    }
    
    public static ItemStack getOfferedItem(UUID playerUUID) {
        return offeredItems.get(playerUUID);
    }
    
    public static void clearOfferedItem(UUID playerUUID) {
        offeredItems.remove(playerUUID);
    }
    
    public static void setSearchingSign(UUID playerUUID, BlockPos signPos) {
        searchingSigns.put(playerUUID, signPos);
        LoggerUtil.info("Player " + playerUUID + " is searching at sign " + signPos);
    }
    
    public static BlockPos getSearchingSign(UUID playerUUID) {
        return searchingSigns.get(playerUUID);
    }
    
    public static void clearSearchingSign(UUID playerUUID) {
        searchingSigns.remove(playerUUID);
    }
    
    /**
     * Procesa el texto del letrero cuando el jugador termina de editarlo
     */
    public static void processSignText(ServerPlayer player, BlockPos signPos, String[] lines) {
        // Verificar si este letrero es uno de búsqueda
        BlockPos searchSign = getSearchingSign(player.getUUID());
        if (searchSign == null || !searchSign.equals(signPos)) {
            return; // No es un letrero de búsqueda
        }
        
        clearSearchingSign(player.getUUID());
        
        // Eliminar el letrero temporal
        player.level.removeBlock(signPos, false);
        
        // Combinar las líneas del letrero en un término de búsqueda
        StringBuilder searchTerm = new StringBuilder();
        for (String line : lines) {
            if (line != null && !line.isEmpty()) {
                if (searchTerm.length() > 0) {
                    searchTerm.append(" ");
                }
                searchTerm.append(line.trim());
            }
        }
        
        String finalSearch = searchTerm.toString().trim();
        LoggerUtil.info("Player " + player.getName().getString() + " searched for: " + finalSearch);
        
        if (finalSearch.isEmpty()) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c¡Debes escribir algo en el letrero!"));
            return;
        }
        
        // Abrir el menú de resultados
        openSearchResultsGUI(player, finalSearch);
    }
    
    /**
     * Busca items en el registro de Minecraft que coincidan con el término de búsqueda
     */
    public static List<Item> searchItems(String searchTerm) {
        List<Item> results = new ArrayList<>();
        
        if (searchTerm == null || searchTerm.isEmpty()) {
            return results;
        }
        
        String lowerSearch = searchTerm.toLowerCase();
        
        // Buscar en todos los items registrados
        for (Item item : ForgeRegistries.ITEMS) {
            ResourceLocation itemKey = ForgeRegistries.ITEMS.getKey(item);
            if (itemKey == null) continue;
            
            String itemName = itemKey.getPath().toLowerCase();
            
            // Si el nombre del item contiene el término de búsqueda
            if (itemName.contains(lowerSearch)) {
                results.add(item);
            }
        }
        
        LoggerUtil.info("Found " + results.size() + " items matching '" + searchTerm + "'");
        return results;
    }
    
    /**
     * Abre un menú mostrando los resultados de la búsqueda
     */
    public static void openSearchResultsGUI(ServerPlayer player, String searchTerm) {
        List<Item> results = searchItems(searchTerm);
        
        if (results.isEmpty()) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c¡No se encontraron items con '" + searchTerm + "'!"));
            BarterUIManager.openCreateOfferGUI(player);
            return;
        }
        
        // Crear menú de 6 filas para mostrar resultados (máximo 54 items)
        SimpleContainer container = new SimpleContainer(54);
        
        int slot = 0;
        for (Item item : results) {
            if (slot >= 54) break;
            
            ItemStack stack = new ItemStack(item);
            ResourceLocation itemKey = ForgeRegistries.ITEMS.getKey(item);
            if (itemKey != null) {
                stack.setHoverName(net.minecraft.network.chat.Component.literal("§e" + itemKey.getPath()));
            }
            container.setItem(slot, stack);
            slot++;
        }
        
        // Marcar que el jugador tiene el menú de búsqueda abierto
        BarterUIManager.putPlayerMenu(player.getUUID(), "search_results");
        
        player.openMenu(new net.minecraft.world.SimpleMenuProvider(
            (windowId, playerInventory, playerEntity) -> 
                new com.barterhouse.menu.BarterChestMenu(
                    net.minecraft.world.inventory.MenuType.GENERIC_9x6, 
                    windowId, 
                    playerInventory, 
                    container, 
                    6, 
                    player, 
                    "search_results"
                ),
            net.minecraft.network.chat.Component.literal("Resultados: " + searchTerm + " (" + results.size() + ")")
        ));
        
        LoggerUtil.info("Opened search results menu for player " + player.getName().getString());
    }
}
