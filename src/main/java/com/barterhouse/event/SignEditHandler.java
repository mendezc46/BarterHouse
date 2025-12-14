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
    
    // Mapa de aliases en español -> inglés para búsqueda
    private static final HashMap<String, String> SEARCH_ALIASES = new HashMap<>();
    
    static {
        // Diamante
        SEARCH_ALIASES.put("diamante", "diamond");
        SEARCH_ALIASES.put("diamantes", "diamond");
        // Oro
        SEARCH_ALIASES.put("oro", "gold");
        // Hierro
        SEARCH_ALIASES.put("hierro", "iron");
        // Piedra
        SEARCH_ALIASES.put("piedra", "stone");
        SEARCH_ALIASES.put("roca", "stone");
        // Madera
        SEARCH_ALIASES.put("madera", "wood");
        // Carbón
        SEARCH_ALIASES.put("carbon", "coal");
        SEARCH_ALIASES.put("carbón", "coal");
        // Esmeralda
        SEARCH_ALIASES.put("esmeralda", "emerald");
        SEARCH_ALIASES.put("esmeraldas", "emerald");
        // Redstone
        SEARCH_ALIASES.put("piedra roja", "redstone");
        // Lapis
        SEARCH_ALIASES.put("lapiz", "lapis");
        SEARCH_ALIASES.put("lápiz", "lapis");
        // Cuarzo
        SEARCH_ALIASES.put("cuarzo", "quartz");
        // Obsidiana
        SEARCH_ALIASES.put("obsidiana", "obsidian");
        // Tierra
        SEARCH_ALIASES.put("tierra", "dirt");
        // Césped
        SEARCH_ALIASES.put("cesped", "grass");
        SEARCH_ALIASES.put("césped", "grass");
        SEARCH_ALIASES.put("pasto", "grass");
        // Arena
        SEARCH_ALIASES.put("arena", "sand");
        // Grava
        SEARCH_ALIASES.put("grava", "gravel");
        // Cristal
        SEARCH_ALIASES.put("cristal", "glass");
        SEARCH_ALIASES.put("vidrio", "glass");
        // Perla
        SEARCH_ALIASES.put("perla", "pearl");
        // Vara
        SEARCH_ALIASES.put("vara", "rod");
        // Polvo
        SEARCH_ALIASES.put("polvo", "dust");
        // Lingote
        SEARCH_ALIASES.put("lingote", "ingot");
        // Pepita
        SEARCH_ALIASES.put("pepita", "nugget");
        // Bloque
        SEARCH_ALIASES.put("bloque", "block");
        // Espada
        SEARCH_ALIASES.put("espada", "sword");
        // Pico
        SEARCH_ALIASES.put("pico", "pickaxe");
        // Hacha
        SEARCH_ALIASES.put("hacha", "axe");
        // Pala
        SEARCH_ALIASES.put("pala", "shovel");
        // Azada
        SEARCH_ALIASES.put("azada", "hoe");
        // Armadura
        SEARCH_ALIASES.put("armadura", "armor");
        // Casco
        SEARCH_ALIASES.put("casco", "helmet");
        // Pechera
        SEARCH_ALIASES.put("pechera", "chestplate");
        // Pantalones
        SEARCH_ALIASES.put("pantalones", "leggings");
        // Botas
        SEARCH_ALIASES.put("botas", "boots");
    }
    
    // Almacena qué jugadores están editando letreros de búsqueda
    private static final HashMap<UUID, BlockPos> searchingSigns = new HashMap<>();
    
    // Almacena el item que el jugador está ofreciendo (el del slot central)
    private static final HashMap<UUID, ItemStack> offeredItems = new HashMap<>();
    
    // Almacena la oferta seleccionada para confirmación
    private static final HashMap<UUID, com.barterhouse.api.TradeOffer> selectedOffers = new HashMap<>();
    
    // Almacena el item seleccionado de búsqueda (antes de especificar cantidad)
    private static final HashMap<UUID, net.minecraft.world.item.ItemStack> selectedSearchItems = new HashMap<>();
    
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
    
    public static void setSelectedOffer(UUID playerUUID, com.barterhouse.api.TradeOffer offer) {
        selectedOffers.put(playerUUID, offer);
        LoggerUtil.info("Stored selected offer for player " + playerUUID);
    }
    
    public static com.barterhouse.api.TradeOffer getSelectedOffer(UUID playerUUID) {
        return selectedOffers.get(playerUUID);
    }
    
    public static void clearSelectedOffer(UUID playerUUID) {
        selectedOffers.remove(playerUUID);
    }
    
    public static void setSelectedSearchItem(UUID playerUUID, net.minecraft.world.item.ItemStack item) {
        selectedSearchItems.put(playerUUID, item.copy());
        LoggerUtil.info("Stored selected search item for player " + playerUUID + ": " + item);
    }
    
    public static net.minecraft.world.item.ItemStack getSelectedSearchItem(UUID playerUUID) {
        return selectedSearchItems.get(playerUUID);
    }
    
    public static void clearSelectedSearchItem(UUID playerUUID) {
        selectedSearchItems.remove(playerUUID);
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
        
        String lowerSearch = searchTerm.toLowerCase().trim();
        
        // Aplicar aliases si existe una traducción
        String translatedSearch = SEARCH_ALIASES.getOrDefault(lowerSearch, lowerSearch);
        
        LoggerUtil.info("Searching for: '" + searchTerm + "' (translated to: '" + translatedSearch + "')");
        
        // Buscar en todos los items registrados
        for (Item item : ForgeRegistries.ITEMS) {
            ResourceLocation itemKey = ForgeRegistries.ITEMS.getKey(item);
            if (itemKey == null) continue;
            
            String itemName = itemKey.getPath().toLowerCase();
            
            // Buscar por ID de minecraft (con término original y traducido)
            if (itemName.contains(lowerSearch) || itemName.contains(translatedSearch)) {
                results.add(item);
                continue;
            }
            
            // Buscar en la translation key
            String translationKey = item.getDescriptionId().toLowerCase();
            if (translationKey.contains(lowerSearch) || translationKey.contains(translatedSearch)) {
                results.add(item);
                continue;
            }
            
            // Buscar en el display name usando ItemStack
            ItemStack stack = new ItemStack(item);
            String displayName = stack.getHoverName().getString().toLowerCase();
            if (displayName.contains(lowerSearch) || displayName.contains(translatedSearch)) {
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
