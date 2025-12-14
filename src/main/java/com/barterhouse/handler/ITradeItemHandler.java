package com.barterhouse.handler;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Interfaz para manejar la selección y validación de items en inventarios.
 */
public interface ITradeItemHandler {

    /**
     * Obtiene un ItemStack filtrado por búsqueda.
     *
     * @param searchQuery Texto de búsqueda
     * @return ItemStack que coincide con la búsqueda
     */
    ItemStack getItemBySearch(String searchQuery);

    /**
     * Valida si el jugador tiene un item específico en su inventario.
     *
     * @param player Jugador a validar
     * @param itemStack Item a buscar
     * @return true si el jugador tiene el item, false en caso contrario
     */
    boolean hasItem(Player player, ItemStack itemStack);

    /**
     * Intenta remover un item del inventario del jugador.
     *
     * @param player Jugador
     * @param itemStack Item a remover
     * @return true si se removió exitosamente, false si no había suficiente cantidad
     */
    boolean removeItem(Player player, ItemStack itemStack);

    /**
     * Intenta agregar un item al inventario del jugador.
     *
     * @param player Jugador
     * @param itemStack Item a agregar
     * @return true si se agregó exitosamente, false si no hay espacio
     */
    boolean addItem(Player player, ItemStack itemStack);

    /**
     * Obtiene el item en la mano del jugador.
     *
     * @param player Jugador
     * @return ItemStack en la mano principal
     */
    ItemStack getMainHandItem(Player player);

    /**
     * Valida si un item es válido para el trueque.
     *
     * @param itemStack Item a validar
     * @return true si es válido, false en caso contrario
     */
    boolean isValidTradeItem(ItemStack itemStack);
}
