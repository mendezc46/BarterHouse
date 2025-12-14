package com.barterhouse.handler;

import com.barterhouse.util.LoggerUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Implementación del manejador de items de trueque.
 * Gestiona la búsqueda, validación y transferencia de items entre jugadores.
 */
public class TradeItemHandler implements ITradeItemHandler {

    private static TradeItemHandler instance;

    private TradeItemHandler() {
    }

    public static synchronized TradeItemHandler getInstance() {
        if (instance == null) {
            instance = new TradeItemHandler();
        }
        return instance;
    }

    @Override
    public ItemStack getItemBySearch(String searchQuery) {
        // Este método sería más complejo en un sistema real
        // Por ahora, retorna un ItemStack vacío como placeholder
        return ItemStack.EMPTY;
    }

    @Override
    public boolean hasItem(Player player, ItemStack itemStack) {
        if (!isValidTradeItem(itemStack)) {
            return false;
        }

        // Buscar el item en el inventario del jugador
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack slot = player.getInventory().getItem(i);
            if (!slot.isEmpty() && slot.getItem() == itemStack.getItem()) {
                // Verificar que la cantidad sea suficiente
                if (slot.getCount() >= itemStack.getCount()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean removeItem(Player player, ItemStack itemStack) {
        if (!isValidTradeItem(itemStack) || !hasItem(player, itemStack)) {
            return false;
        }

        int remaining = itemStack.getCount();
        
        // Remover items del inventario comenzando desde el final
        for (int i = player.getInventory().getContainerSize() - 1; i >= 0 && remaining > 0; i--) {
            ItemStack slot = player.getInventory().getItem(i);
            
            if (!slot.isEmpty() && slot.getItem() == itemStack.getItem()) {
                int toRemove = Math.min(slot.getCount(), remaining);
                slot.shrink(toRemove);
                remaining -= toRemove;
            }
        }

        LoggerUtil.debug("Removed " + itemStack.getCount() + " of " + 
                        itemStack.getHoverName().getString() + " from " + player.getName().getString());
        return remaining == 0;
    }

    @Override
    public boolean addItem(Player player, ItemStack itemStack) {
        if (!isValidTradeItem(itemStack)) {
            return false;
        }

        ItemStack copy = itemStack.copy();
        return player.addItem(copy);
    }

    @Override
    public ItemStack getMainHandItem(Player player) {
        return player.getMainHandItem();
    }

    @Override
    public boolean isValidTradeItem(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return false;
        }

        // No permitir items que se crían naturalmente (bloques como bedrock, etc)
        if (itemStack.getItem() == Items.AIR || 
            itemStack.getItem() == Items.BEDROCK ||
            itemStack.getItem() == Items.BARRIER ||
            itemStack.getItem() == Items.COMMAND_BLOCK ||
            itemStack.getItem() == Items.CHAIN_COMMAND_BLOCK ||
            itemStack.getItem() == Items.REPEATING_COMMAND_BLOCK) {
            return false;
        }

        return true;
    }
}
