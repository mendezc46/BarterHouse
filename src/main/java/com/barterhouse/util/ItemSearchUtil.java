package com.barterhouse.util;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilidad para búsqueda de items del juego.
 * Proporciona métodos para buscar items por nombre y obtener información de items.
 */
public class ItemSearchUtil {

    /**
     * Busca items que coincidan con el nombre proporcionado.
     *
     * @param query Término de búsqueda
     * @return Lista de ItemStacks que coinciden
     */
    public static List<ItemStack> searchItems(String query) {
        List<ItemStack> results = new ArrayList<>();
        
        if (query == null || query.trim().isEmpty()) {
            return results;
        }

        String lowerQuery = query.toLowerCase().trim();

        // Iterar sobre todos los items registrados
        for (Item item : ForgeRegistries.ITEMS) {
            String itemName = item.getDescription().getString().toLowerCase();
            net.minecraft.resources.ResourceLocation registryLoc = ForgeRegistries.ITEMS.getKey(item);
            String registryName = registryLoc != null ? registryLoc.toString().toLowerCase() : "";

            // Buscar coincidencias parciales
            if (itemName.contains(lowerQuery) || registryName.contains(lowerQuery)) {
                results.add(new ItemStack(item));
            }
        }

        return results;
    }

    /**
     * Obtiene un item por su nombre de registro.
     *
     * @param registryName Nombre del registro (ej: minecraft:diamond)
     * @return ItemStack del item, o EMPTY si no existe
     */
    public static ItemStack getItemByRegistryName(String registryName) {
        Item item = ForgeRegistries.ITEMS.getValue(
                new net.minecraft.resources.ResourceLocation(registryName)
        );
        
        if (item != null) {
            return new ItemStack(item);
        }
        
        return ItemStack.EMPTY;
    }

    /**
     * Obtiene el nombre visual de un item.
     *
     * @param itemStack ItemStack a procesar
     * @return Nombre visual del item
     */
    public static String getItemDisplayName(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return "Aire";
        }
        return itemStack.getHoverName().getString();
    }

    /**
     * Obtiene el nombre de registro de un item.
     *
     * @param itemStack ItemStack a procesar
     * @return Nombre de registro (ej: minecraft:diamond)
     */
    public static String getItemRegistryName(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return "minecraft:air";
        }
        net.minecraft.resources.ResourceLocation registryLoc = ForgeRegistries.ITEMS.getKey(itemStack.getItem());
        return registryLoc != null ? registryLoc.toString() : "minecraft:air";
    }

    /**
     * Valida que dos items sean del mismo tipo.
     *
     * @param item1 Primer item
     * @param item2 Segundo item
     * @return true si son del mismo tipo
     */
    public static boolean isSameItem(ItemStack item1, ItemStack item2) {
        if (item1.isEmpty() || item2.isEmpty()) {
            return false;
        }
        return item1.getItem() == item2.getItem();
    }
}
