package com.barterhouse.menu;

import com.barterhouse.commands.BarterUIManager;
import com.barterhouse.util.LoggerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;

import java.util.UUID;

/**
 * ChestMenu personalizado que detecta clicks en botones - SERVER SIDE ONLY
 */
public class BarterChestMenu extends ChestMenu {
    
    private final String menuType;
    private final ServerPlayer player;
    
    public BarterChestMenu(MenuType<?> menuType, int windowId, Inventory playerInventory, Container container, int rows, ServerPlayer player, String menuTypeStr) {
        super(menuType, windowId, playerInventory, container, rows);
        this.menuType = menuTypeStr;
        this.player = player;
    }
    
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        LoggerUtil.info("Clicked slot: " + slotId + " in menu: " + menuType);
        
        // Detectar clicks en botones (slots 45 y 46 en menus de 6 filas)
        if (menuType.equals("offers")) {
            if (slotId == 45) {
                // Botón Crear Oferta (Esmeralda)
                LoggerUtil.info("Opening Create Offer menu");
                player.closeContainer();
                player.getServer().execute(() -> BarterUIManager.openCreateOfferGUI(player));
                return; // Cancelar el click para que no tome el item
            } else if (slotId == 46) {
                // Botón Borrar Ofertas (Redstone)
                LoggerUtil.info("Opening Delete Offers menu");
                player.closeContainer();
                player.getServer().execute(() -> BarterUIManager.openDeleteMyOffersGUI(player));
                return; // Cancelar el click
            }
        } else if (menuType.equals("delete")) {
            if (slotId == 45) {
                // Botón Volver (Flecha)
                LoggerUtil.info("Going back to offers menu");
                player.closeContainer();
                player.getServer().execute(() -> BarterUIManager.openOffersListGUI(player));
                return; // Cancelar el click
            }
        } else if (menuType.equals("create")) {
            if (slotId == 18) {
                // Click en el letrero para buscar item
                LoggerUtil.info("Opening sign editor for item search");
                
                // Guardar el item ofrecido (del slot 13) antes de cerrar el menú
                ItemStack offeredItem = this.getSlot(13).getItem();
                if (offeredItem.isEmpty()) {
                    player.displayClientMessage(Component.literal("§c¡Debes poner un item en el centro primero!"), true);
                    return; // No abrir el letrero si no hay item
                }
                
                com.barterhouse.event.SignEditHandler.setOfferedItem(player.getUUID(), offeredItem);
                
                player.closeContainer();
                player.getServer().execute(() -> openSignEditor((ServerPlayer) player));
                return; // Cancelar el click
            }
            
            // Permitir clicks en el slot 13 (centro) para poner item
            if (slotId == 13) {
                super.clicked(slotId, button, clickType, player);
                return;
            }
            
            // Bloquear clicks en los slots de vidrio (3,4,5,12,14,21,22,23) y letrero (18)
            if (slotId == 3 || slotId == 4 || slotId == 5 || 
                slotId == 12 || slotId == 14 || 
                slotId == 21 || slotId == 22 || slotId == 23 || slotId == 18) {
                return; // Cancelar click
            }
        } else if (menuType.equals("search_results")) {
            // Click en el menú de resultados - crear oferta
            ItemStack clickedItem = this.getSlot(slotId).getItem();
            
            if (clickedItem.isEmpty()) {
                return; // No hay item en este slot
            }
            
            LoggerUtil.info("Player clicked on item: " + clickedItem + " in search results");
            
            // Obtener el item ofrecido guardado
            ItemStack offeredItem = com.barterhouse.event.SignEditHandler.getOfferedItem(player.getUUID());
            
            if (offeredItem == null || offeredItem.isEmpty()) {
                player.displayClientMessage(Component.literal("§cError: No se encontró el item ofrecido"), true);
                player.closeContainer();
                return;
            }
            
            // Crear la oferta
            player.closeContainer();
            ServerPlayer serverPlayer = (ServerPlayer) player;
            serverPlayer.getServer().execute(() -> createOffer(serverPlayer, offeredItem, clickedItem));
            return; // Cancelar click
        }
        
        // Para cualquier otro slot, comportamiento normal
        super.clicked(slotId, button, clickType, player);
    }
    
    private void createOffer(ServerPlayer player, ItemStack offeredItem, ItemStack requestedItem) {
        try {
            // Crear la oferta usando el manager
            UUID offerId = com.barterhouse.manager.TradeOfferManager.getInstance().createOffer(
                player.getUUID(),
                player.getName().getString(),
                offeredItem.copy(),
                requestedItem.copy()
            );
            
            // Limpiar el item guardado
            com.barterhouse.event.SignEditHandler.clearOfferedItem(player.getUUID());
            
            // Mensaje de confirmación
            player.displayClientMessage(Component.literal("§a¡Oferta creada exitosamente!"), false);
            player.displayClientMessage(Component.literal("§7Ofreces: §e" + offeredItem.getCount() + "x " + offeredItem.getDisplayName().getString()), false);
            player.displayClientMessage(Component.literal("§7Pides: §e" + requestedItem.getCount() + "x " + requestedItem.getDisplayName().getString()), false);
            
            LoggerUtil.info("Offer created successfully for player " + player.getName().getString() + " with ID: " + offerId);
            
            // Volver al menú principal
            BarterUIManager.openOffersListGUI(player);
            
        } catch (Exception e) {
            LoggerUtil.error("Error creating offer: " + e.getMessage());
            e.printStackTrace();
            player.displayClientMessage(Component.literal("§cError al crear la oferta"), true);
        }
    }
    
    private void openSignEditor(ServerPlayer player) {
        // Crear un letrero temporal en el mundo en la posición del jugador
        BlockPos signPos = player.blockPosition().above(10); // Alto para que no interfiera
        
        // Colocar un letrero temporal invisible
        player.level.setBlock(signPos, net.minecraft.world.level.block.Blocks.OAK_SIGN.defaultBlockState(), 3);
        
        // Obtener el SignBlockEntity
        net.minecraft.world.level.block.entity.BlockEntity blockEntity = player.level.getBlockEntity(signPos);
        if (blockEntity instanceof net.minecraft.world.level.block.entity.SignBlockEntity) {
            net.minecraft.world.level.block.entity.SignBlockEntity signEntity = (net.minecraft.world.level.block.entity.SignBlockEntity) blockEntity;
            
            // Marcar que este jugador está editando un letrero de búsqueda
            com.barterhouse.event.SignEditHandler.setSearchingSign(player.getUUID(), signPos);
            
            // Abrir el editor de letrero
            player.openTextEdit(signEntity);
            
            LoggerUtil.info("Sign editor opened at " + signPos);
        }
    }
}
