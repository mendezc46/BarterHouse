package com.barterhouse.menu;

import com.barterhouse.commands.BarterUIManager;
import com.barterhouse.config.MessageConfig;
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
import java.util.HashMap;

/**
 * ChestMenu personalizado que detecta clicks en botones - SERVER SIDE ONLY
 */
public class BarterChestMenu extends ChestMenu {
    
    private final String menuType;
    private final ServerPlayer player;
    
    // HashMap para guardar cantidades mayores a 64 de jugadores
    private static final HashMap<UUID, Integer> playerQuantities = new HashMap<>();
    
    /**
     * Obtiene la cantidad real de un ItemStack, considerando el NBT ActualCount si existe
     * @param stack El ItemStack a verificar
     * @return La cantidad real (puede ser > 64)
     */
    private static int getActualCount(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains("ActualCount")) {
            return stack.getTag().getInt("ActualCount");
        }
        return stack.getCount();
    }
    
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
            } else if (slotId == 47) {
                // Botón Bodega (Cofre)
                LoggerUtil.info("Opening Warehouse menu");
                player.closeContainer();
                player.getServer().execute(() -> BarterUIManager.openWarehouseGUI(player));
                return; // Cancelar el click
            } else if (slotId < 36) {
                // Click en una oferta (primeras 4 filas)
                ItemStack clickedItem = this.getSlot(slotId).getItem();
                if (!clickedItem.isEmpty()) {
                    LoggerUtil.info("Player clicked on offer at slot " + slotId);
                    player.closeContainer();
                    player.getServer().execute(() -> openOfferConfirmationMenu((ServerPlayer) player, slotId));
                    return; // Cancelar click
                }
            }
        } else if (menuType.equals("delete")) {
            if (slotId == 45) {
                // Botón Volver (Flecha)
                LoggerUtil.info("Going back to offers menu");
                player.closeContainer();
                player.getServer().execute(() -> BarterUIManager.openOffersListGUI(player));
                return; // Cancelar el click
            } else if (slotId < 36) {
                // Click en una oferta (primeras 4 filas) para eliminar
                ItemStack clickedItem = this.getSlot(slotId).getItem();
                if (!clickedItem.isEmpty()) {
                    LoggerUtil.info("Player clicked on offer to delete at slot " + slotId);
                    player.closeContainer();
                    player.getServer().execute(() -> openDeleteConfirmationMenu((ServerPlayer) player, slotId));
                    return; // Cancelar click
                }
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
        } else if (menuType.equals("confirm_offer")) {
            if (slotId == 10) {
                // Botón Aceptar
                LoggerUtil.info("Player accepted offer");
                player.closeContainer();
                player.getServer().execute(() -> acceptOffer((ServerPlayer) player));
                return;
            } else if (slotId == 16) {
                // Botón Cancelar
                LoggerUtil.info("Player cancelled offer");
                player.closeContainer();
                com.barterhouse.event.SignEditHandler.clearSelectedOffer(player.getUUID());
                player.getServer().execute(() -> BarterUIManager.openOffersListGUI(player));
                return;
            }
            // Bloquear otros clicks
            return;
        } else if (menuType.equals("confirm_delete")) {
            if (slotId == 10) {
                // Botón Confirmar eliminación (SÍ)
                LoggerUtil.info("Player confirmed deletion");
                player.closeContainer();
                player.getServer().execute(() -> deleteOffer((ServerPlayer) player));
                return;
            } else if (slotId == 16) {
                // Botón Cancelar eliminación (NO)
                LoggerUtil.info("Player cancelled deletion");
                player.closeContainer();
                com.barterhouse.event.SignEditHandler.clearSelectedOffer(player.getUUID());
                player.getServer().execute(() -> BarterUIManager.openDeleteMyOffersGUI(player));
                return;
            }
            // Bloquear otros clicks
            return;
        } else if (menuType.equals("warehouse")) {
            if (slotId == 45) {
                // Botón Volver (Flecha)
                LoggerUtil.info("Going back to offers menu");
                player.closeContainer();
                player.getServer().execute(() -> BarterUIManager.openOffersListGUI(player));
                return;
            } else if (slotId < 36) {
                // Click en un item de la bodega para sacarlo
                ItemStack clickedItem = this.getSlot(slotId).getItem();
                if (!clickedItem.isEmpty()) {
                    LoggerUtil.info("Player withdrawing item from warehouse at slot " + slotId);
                    player.closeContainer();
                    player.getServer().execute(() -> withdrawFromWarehouse((ServerPlayer) player, slotId));
                    return;
                }
            }
            // Bloquear otros clicks
            return;
        } else if (menuType.equals("search_results")) {
            // Click en el menú de resultados - abrir menú de cantidad
            ItemStack clickedItem = this.getSlot(slotId).getItem();
            
            if (clickedItem.isEmpty()) {
                return; // No hay item en este slot
            }
            
            LoggerUtil.info("Player clicked on item: " + clickedItem + " in search results");
            
            // Guardar el item seleccionado
            com.barterhouse.event.SignEditHandler.setSelectedSearchItem(player.getUUID(), clickedItem);
            
            // Abrir menú de cantidad
            player.closeContainer();
            ServerPlayer serverPlayer = (ServerPlayer) player;
            serverPlayer.getServer().execute(() -> openQuantitySelectionMenu(serverPlayer, clickedItem));
            return; // Cancelar click
        } else if (menuType.equals("quantity_selection")) {
            // Click en menú de selección de cantidad con botones rápidos +/-
            ItemStack quantityItem = this.getSlot(4).getItem();
            
            // Obtener la cantidad actual del HashMap
            int currentQuantity = playerQuantities.getOrDefault(player.getUUID(), 1);
            
            // Botones de disminuir: slots 0, 1, 2 (-1, -10, -30)
            if (slotId == 0) {
                // -1
                if (currentQuantity > 1) {
                    currentQuantity--;
                }
                LoggerUtil.info("Player decreased quantity by 1: " + currentQuantity);
            } else if (slotId == 1) {
                // -10
                currentQuantity = Math.max(1, currentQuantity - 10);
                LoggerUtil.info("Player decreased quantity by 10: " + currentQuantity);
            } else if (slotId == 2) {
                // -30
                currentQuantity = Math.max(1, currentQuantity - 30);
                LoggerUtil.info("Player decreased quantity by 30: " + currentQuantity);
            }
            // Botones de aumentar: slots 6, 7, 8 (+1, +10, +30)
            else if (slotId == 6) {
                // +1
                if (currentQuantity < 999) {
                    currentQuantity++;
                }
                LoggerUtil.info("Player increased quantity by 1: " + currentQuantity);
            } else if (slotId == 7) {
                // +10
                currentQuantity = Math.min(999, currentQuantity + 10);
                LoggerUtil.info("Player increased quantity by 10: " + currentQuantity);
            } else if (slotId == 8) {
                // +30
                currentQuantity = Math.min(999, currentQuantity + 30);
                LoggerUtil.info("Player increased quantity by 30: " + currentQuantity);
            }
            // Botón Confirmar: slot 13
            else if (slotId == 13) {
                // Obtener la cantidad real del HashMap
                int quantity = playerQuantities.getOrDefault(player.getUUID(), 1);
                
                LoggerUtil.info("Player confirmed quantity: " + quantity);
                
                // Obtener el item ofrecido guardado
                ItemStack offeredItem = com.barterhouse.event.SignEditHandler.getOfferedItem(player.getUUID());
                
                if (offeredItem == null || offeredItem.isEmpty()) {
                    player.displayClientMessage(Component.literal(MessageConfig.getInstance().get("errors.offered_item_error")), true);
                    player.closeContainer();
                    playerQuantities.remove(player.getUUID());
                    return;
                }
                
                // Obtener el item solicitado
                ItemStack requestedItem = com.barterhouse.event.SignEditHandler.getSelectedSearchItem(player.getUUID());
                
                if (requestedItem == null || requestedItem.isEmpty()) {
                    player.displayClientMessage(Component.literal(MessageConfig.getInstance().get("errors.requested_item_error")), true);
                    player.closeContainer();
                    playerQuantities.remove(player.getUUID());
                    return;
                }
                
                // Guardar la cantidad real en el NBT del item (para cantidades > 64)
                requestedItem.setCount(Math.min(64, quantity));
                
                // Guardar la cantidad real en NBT para persistencia
                net.minecraft.nbt.CompoundTag nbt = requestedItem.getOrCreateTag();
                nbt.putInt("ActualCount", quantity);
                
                // Crear la oferta
                player.closeContainer();
                ServerPlayer serverPlayer = (ServerPlayer) player;
                serverPlayer.getServer().execute(() -> createOffer(serverPlayer, offeredItem, requestedItem));
                com.barterhouse.event.SignEditHandler.clearSelectedSearchItem(player.getUUID());
                playerQuantities.remove(player.getUUID());
                return;
            }
            // Botón Cancelar: slot 21
            else if (slotId == 21) {
                LoggerUtil.info("Player cancelled quantity selection");
                player.closeContainer();
                ServerPlayer serverPlayer = (ServerPlayer) player;
                com.barterhouse.event.SignEditHandler.clearSelectedSearchItem(player.getUUID());
                playerQuantities.remove(player.getUUID());
                serverPlayer.getServer().execute(() -> BarterUIManager.openCreateOfferGUI(serverPlayer));
                return;
            }
            
            // Actualizar el item en el slot 4 con la nueva cantidad
            if (slotId >= 0 && slotId <= 8) {
                // Guardar la cantidad en el HashMap
                playerQuantities.put(player.getUUID(), currentQuantity);
                
                // Mantener el item visual en 1 para evitar que desaparezca
                quantityItem.setCount(1);
                quantityItem.setHoverName(Component.literal("§e§lCantidad: §f" + currentQuantity + "x\n§7Cantidad actual solicitada"));
            }
            
            return;
        }
        
        // Manejo de colocación de items:
        // - Permitir slot -999 (dropear fuera del GUI)
        // - Permitir inventario del jugador (slots >= 54)
        // - Permitir slot 13 en menú "create"
        // - Bloquear SOLO colocar items en slots del GUI (0-53)
        
        // Slot -999 es dropear item fuera del GUI - siempre permitir
        if (slotId == -999) {
            super.clicked(slotId, button, clickType, player);
            return;
        }
        
        // Inventario del jugador (slots >= 54) - siempre permitir todas las interacciones
        if (slotId >= 54) {
            super.clicked(slotId, button, clickType, player);
            return;
        }
        
        // Slot 13 del menú "create" - permitir colocar items
        if (menuType.equals("create") && slotId == 13) {
            super.clicked(slotId, button, clickType, player);
            return;
        }
        
        // Para slots del GUI (0-53): permitir sacar items, pero bloquear colocarlos
        ItemStack cursorItem = this.getCarried();
        
        // Si el cursor tiene un item = intento de colocar → bloquear
        // Si el cursor está vacío = intento de sacar → permitir
        if (!cursorItem.isEmpty()) {
            return; // Bloquear colocar items en slots del GUI
        }
        
        // Permitir todo lo demás (sacar items, clicks con cursor vacío)
        super.clicked(slotId, button, clickType, player);
    }
    
    @Override
    public void removed(Player player) {
        super.removed(player);
        
        // Limpiar HashMap de cantidades
        playerQuantities.remove(player.getUUID());
        
        // Cuando el menú se cierra, devolver items
        if (menuType.equals("create")) {
            // Slot 13 es donde el jugador pone el item a ofrecer
            ItemStack itemInSlot = this.getSlot(13).getItem();
            
            if (!itemInSlot.isEmpty()) {
                // Verificar si el item está guardado en memoria (está buscando un item solicitado)
                ItemStack savedItem = com.barterhouse.event.SignEditHandler.getOfferedItem(player.getUUID());
                
                if (savedItem != null && !savedItem.isEmpty()) {
                    // El jugador está en proceso de crear oferta (buscando item) → NO devolver
                    LoggerUtil.info("Item kept in memory for player " + player.getName().getString() + " (searching for requested item)");
                    this.getSlot(13).set(ItemStack.EMPTY);
                    return;
                }
                
                // Verificar si el jugador se está desconectando
                if (com.barterhouse.event.PlayerDisconnectTracker.isDisconnecting(player.getUUID())) {
                    // Jugador desconectado → Enviar a bodega
                    com.barterhouse.manager.WarehouseManager.getInstance()
                        .addItem(player.getUUID(), itemInSlot.copy(), "Sistema");
                    LoggerUtil.info("Sent item from slot 13 to warehouse (disconnected) for player " + player.getName().getString());
                } else {
                    // Jugador conectado → Devolver al inventario
                    if (!player.getInventory().add(itemInSlot.copy())) {
                        // Si el inventario está lleno, dropear
                        player.drop(itemInSlot, false);
                    }
                    LoggerUtil.info("Returned item from slot 13 to inventory for player " + player.getName().getString());
                }
                
                // Limpiar el slot
                this.getSlot(13).set(ItemStack.EMPTY);
            }
        }
        
        // Limpiar el item guardado en memoria solo si NO está buscando
        ItemStack savedItem = com.barterhouse.event.SignEditHandler.getOfferedItem(player.getUUID());
        if (savedItem == null || savedItem.isEmpty()) {
            com.barterhouse.event.SignEditHandler.clearOfferedItem(player.getUUID());
        }
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
            
            // Limpiar el item guardado (memoria)
            com.barterhouse.event.SignEditHandler.clearOfferedItem(player.getUUID());
            
            // Mensaje de confirmación
            player.displayClientMessage(Component.literal("§a¡Oferta creada exitosamente!"), false);
            player.displayClientMessage(Component.literal("§7Ofreces: §e" + getActualCount(offeredItem) + "x " + offeredItem.getDisplayName().getString()), false);
            player.displayClientMessage(Component.literal("§7Pides: §e" + getActualCount(requestedItem) + "x " + requestedItem.getDisplayName().getString()), false);
            
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
    
    private void openQuantitySelectionMenu(ServerPlayer player, ItemStack selectedItem) {
        // Crear menú de 3 filas (27 slots) para seleccionar cantidad con botones rápidos
        net.minecraft.world.SimpleContainer container = new net.minecraft.world.SimpleContainer(27);
        
        MessageConfig config = MessageConfig.getInstance();
        
        // Cantidad inicial: 1
        int initialQuantity = 1;
        
        // Guardar la cantidad en el HashMap del jugador
        playerQuantities.put(player.getUUID(), initialQuantity);
        
        // Fila 1: Botones de disminuir (-1, -10, -30) en slots 0, 1, 2
        ItemStack decreaseOne = new ItemStack(Items.RED_CONCRETE);
        decreaseOne.setHoverName(Component.literal("§c§l- 1"));
        container.setItem(0, decreaseOne);
        
        ItemStack decreaseTen = new ItemStack(Items.ORANGE_CONCRETE);
        decreaseTen.setHoverName(Component.literal("§6§l- 10"));
        container.setItem(1, decreaseTen);
        
        ItemStack decreaseThirty = new ItemStack(Items.YELLOW_CONCRETE);
        decreaseThirty.setHoverName(Component.literal("§e§l- 30"));
        container.setItem(2, decreaseThirty);
        
        // Fila 1: Item con cantidad en slot 4 (centro)
        ItemStack displayItem = selectedItem.copy();
        displayItem.setCount(1); // Siempre mostrar 1 visualmente
        displayItem.setHoverName(Component.literal("§e§lCantidad: §f" + initialQuantity + "x\n§7Cantidad actual solicitada"));
        container.setItem(4, displayItem);
        
        // Fila 1: Botones de aumentar (+1, +10, +30) en slots 6, 7, 8
        ItemStack increaseOne = new ItemStack(Items.LIME_CONCRETE);
        increaseOne.setHoverName(Component.literal("§a§l+ 1"));
        container.setItem(6, increaseOne);
        
        ItemStack increaseTen = new ItemStack(Items.GREEN_CONCRETE);
        increaseTen.setHoverName(Component.literal("§2§l+ 10"));
        container.setItem(7, increaseTen);
        
        ItemStack increaseThirty = new ItemStack(Items.LIME_STAINED_GLASS);
        increaseThirty.setHoverName(Component.literal("§0§l+ 30"));
        container.setItem(8, increaseThirty);
        
        // Fila 2: Botón Confirmar en slot 13 (centro)
        ItemStack confirmButton = new ItemStack(Items.EMERALD);
        confirmButton.setHoverName(Component.literal("§a§lConfirmar Cantidad"));
        container.setItem(13, confirmButton);
        
        // Fila 3: Botón Cancelar en slot 21
        ItemStack cancelButton = new ItemStack(Items.BARRIER);
        cancelButton.setHoverName(Component.literal(config.get("buttons.cancel")));
        container.setItem(21, cancelButton);
        
        // Guardar el item seleccionado
        com.barterhouse.event.SignEditHandler.setSelectedSearchItem(player.getUUID(), selectedItem.copy());
        
        BarterUIManager.putPlayerMenu(player.getUUID(), "quantity_selection");
        
        player.openMenu(new net.minecraft.world.SimpleMenuProvider(
            (windowId, playerInventory, playerEntity) -> 
                new BarterChestMenu(
                    net.minecraft.world.inventory.MenuType.GENERIC_9x3,
                    windowId,
                    playerInventory,
                    container,
                    3,
                    player,
                    "quantity_selection"
                ),
            Component.literal(config.get("menu.quantity_title"))
        ));
    }
    
    private void openOfferConfirmationMenu(ServerPlayer player, int offerSlot) {
        MessageConfig config = MessageConfig.getInstance();
        
        // Obtener la lista de ofertas activas
        java.util.List<com.barterhouse.api.TradeOffer> allOffers = 
            com.barterhouse.manager.TradeOfferManager.getInstance().getAllActiveOffers();
        
        if (offerSlot >= allOffers.size()) {
            player.displayClientMessage(Component.literal(config.get("errors.offer_not_found")), true);
            return;
        }
        
        com.barterhouse.api.TradeOffer offer = allOffers.get(offerSlot);
        
        // Guardar temporalmente la oferta seleccionada
        com.barterhouse.event.SignEditHandler.setSelectedOffer(player.getUUID(), offer);
        
        // Crear menú de 2 filas para confirmación
        net.minecraft.world.SimpleContainer container = new net.minecraft.world.SimpleContainer(18);
        
        // Información sobre lo que obtendrás
        ItemStack offeredItemDisplay = offer.getOfferedItem().copy();
        offeredItemDisplay.setCount(1);
        StringBuilder offeredHover = new StringBuilder();
        offeredHover.append("§e§lRECIBES:\\n");
        offeredHover.append("§7Cantidad: §e").append(getActualCount(offer.getOfferedItem())).append("x\\n");
        offeredHover.append("§7Item: §e").append(offer.getOfferedItem().getDisplayName().getString());
        offeredItemDisplay.setHoverName(Component.literal(offeredHover.toString()));
        container.setItem(3, offeredItemDisplay);
        
        ItemStack requestedItemDisplay = offer.getRequestedItem().copy();
        requestedItemDisplay.setCount(1);
        StringBuilder requestedHover = new StringBuilder();
        requestedHover.append("§c§lNECESITAS:\\n");
        requestedHover.append("§7Cantidad: §c").append(getActualCount(offer.getRequestedItem())).append("x\\n");
        requestedHover.append("§7Item: §c").append(offer.getRequestedItem().getDisplayName().getString());
        requestedItemDisplay.setHoverName(Component.literal(requestedHover.toString()));
        container.setItem(5, requestedItemDisplay);
        
        // Botón Aceptar (Verde)
        ItemStack acceptButton = new ItemStack(Items.GREEN_CONCRETE);
        acceptButton.setHoverName(Component.literal(config.get("buttons.accept")));
        container.setItem(10, acceptButton);
        
        // Botón Cancelar (Rojo)
        ItemStack cancelButton = new ItemStack(Items.RED_CONCRETE);
        cancelButton.setHoverName(Component.literal(config.get("buttons.cancel")));
        container.setItem(16, cancelButton);
        
        BarterUIManager.putPlayerMenu(player.getUUID(), "confirm_offer");
        
        player.openMenu(new net.minecraft.world.SimpleMenuProvider(
            (windowId, playerInventory, playerEntity) -> 
                new BarterChestMenu(
                    net.minecraft.world.inventory.MenuType.GENERIC_9x2,
                    windowId,
                    playerInventory,
                    container,
                    2,
                    player,
                    "confirm_offer"
                ),
            Component.literal(config.get("menu.confirm_title"))
        ));
    }
    
    private void acceptOffer(ServerPlayer player) {
        try {
            MessageConfig config = MessageConfig.getInstance();
            
            // Obtener la oferta guardada
            com.barterhouse.api.TradeOffer offer = com.barterhouse.event.SignEditHandler.getSelectedOffer(player.getUUID());
            
            if (offer == null) {
                player.displayClientMessage(Component.literal(config.get("errors.offer_not_found")), true);
                BarterUIManager.openOffersListGUI(player);
                return;
            }
            
            // Verificar que el jugador no es el creador de la oferta
            if (offer.getCreatorUUID().equals(player.getUUID())) {
                player.displayClientMessage(Component.literal(config.get("errors.cannot_accept_own_offer")), true);
                BarterUIManager.openOffersListGUI(player);
                return;
            }
            
            // Obtener el item que el jugador necesita tener
            ItemStack requiredItem = offer.getRequestedItem().copy();
            
            // Verificar que el jugador tiene el item
            int itemCount = 0;
            for (ItemStack stack : player.getInventory().items) {
                if (!stack.isEmpty() && stack.sameItem(requiredItem)) {
                    itemCount += stack.getCount();
                }
            }
            
            // También verificar en el cursor del jugador
            ItemStack cursorItem = player.containerMenu.getCarried();
            if (!cursorItem.isEmpty() && cursorItem.sameItem(requiredItem)) {
                itemCount += cursorItem.getCount();
            }
            
            int required = getActualCount(requiredItem);
            
            if (itemCount < required) {
                // Cerrar menú primero
                player.closeContainer();
                
                // Mensajes claros en el chat (no en action bar)
                String itemName = requiredItem.getDisplayName().getString();
                String separator = config.get("errors.insufficient_items_separator");
                
                player.sendSystemMessage(Component.literal(""));
                player.sendSystemMessage(Component.literal("§c" + separator));
                player.sendSystemMessage(Component.literal(config.get("errors.insufficient_items_title")));
                player.sendSystemMessage(Component.literal(""));
                player.sendSystemMessage(Component.literal(config.get("errors.insufficient_items_item", "item", itemName)));
                player.sendSystemMessage(Component.literal(config.get("errors.insufficient_items_required", "required", required)));
                player.sendSystemMessage(Component.literal(config.get("errors.insufficient_items_have", "have", itemCount)));
                player.sendSystemMessage(Component.literal(config.get("errors.insufficient_items_missing", "missing", (required - itemCount), "item", itemName)));
                player.sendSystemMessage(Component.literal(""));
                player.sendSystemMessage(Component.literal("§c" + separator));
                player.sendSystemMessage(Component.literal(""));
                
                com.barterhouse.event.SignEditHandler.clearSelectedOffer(player.getUUID());
                
                // Volver al menú después de un pequeño delay
                player.getServer().execute(() -> {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    BarterUIManager.openOffersListGUI(player);
                });
                return;
            }
            
            // Remover el item requerido del inventario
            ItemStack toRemove = requiredItem.copy();
            int remaining = getActualCount(requiredItem);
            
            for (int i = 0; i < player.getInventory().items.size() && remaining > 0; i++) {
                ItemStack stack = player.getInventory().items.get(i);
                if (!stack.isEmpty() && stack.sameItem(toRemove)) {
                    int taken = Math.min(stack.getCount(), remaining);
                    stack.shrink(taken);
                    remaining -= taken;
                }
            }
            
            // Si aún queda, remover del cursor
            if (remaining > 0 && !cursorItem.isEmpty() && cursorItem.sameItem(toRemove)) {
                int taken = Math.min(cursorItem.getCount(), remaining);
                cursorItem.shrink(taken);
                remaining -= taken;
            }
            
            // Agregar el item ofrecido a la BODEGA del comprador (NO al inventario)
            ItemStack offeredItem = offer.getOfferedItem().copy();
            com.barterhouse.manager.WarehouseManager.getInstance().addItem(
                player.getUUID(), 
                offeredItem, 
                offer.getCreatorName()
            );
            
            // Agregar el item solicitado (que el comprador dio) a la BODEGA del vendedor
            ItemStack requestedItem = offer.getRequestedItem().copy();
            com.barterhouse.manager.WarehouseManager.getInstance().addItem(
                offer.getCreatorUUID(),
                requestedItem,
                player.getName().getString()
            );
            
            // Registrar la transacción (marcar la oferta como aceptada o eliminarla)
            com.barterhouse.manager.TradeOfferManager.getInstance().removeOffer(offer.getOfferId());
            
            // Limpiar la oferta guardada
            com.barterhouse.event.SignEditHandler.clearSelectedOffer(player.getUUID());
            
            // Mensajes de confirmación
            player.sendSystemMessage(Component.literal(config.get("success.offer_accepted")));
            player.sendSystemMessage(Component.literal(config.get("success.offer_sent_to_warehouse", "count", getActualCount(offeredItem), "item", offeredItem.getDisplayName().getString())));
            
            LoggerUtil.info("Offer " + offer.getOfferId() + " accepted by player " + player.getName().getString());
            
            // Volver al menú principal
            BarterUIManager.openOffersListGUI(player);
            
        } catch (Exception e) {
            LoggerUtil.error("Error accepting offer: " + e.getMessage());
            e.printStackTrace();
            MessageConfig configErr = MessageConfig.getInstance();
            player.displayClientMessage(Component.literal(configErr.get("errors.accept_error")), true);
        }
    }
    
    /**
     * Abre el menú de confirmación para eliminar una oferta
     */
    private void openDeleteConfirmationMenu(ServerPlayer player, int slotId) {
        try {
            MessageConfig config = MessageConfig.getInstance();
            
            // Obtener todas las ofertas del jugador
            java.util.List<com.barterhouse.api.TradeOffer> myOffers = 
                com.barterhouse.manager.TradeOfferManager.getInstance()
                    .getPlayerOffers(player.getUUID());
            
            if (slotId >= myOffers.size()) {
                player.displayClientMessage(Component.literal(config.get("errors.offer_not_found")), true);
                return;
            }
            
            // Obtener la oferta específica
            com.barterhouse.api.TradeOffer selectedOffer = myOffers.get(slotId);
            
            // Guardar la oferta para cuando el jugador confirme
            com.barterhouse.event.SignEditHandler.setSelectedOffer(player.getUUID(), selectedOffer);
            
            // Crear el menú de confirmación (2 filas)
            net.minecraft.world.SimpleContainer container = new net.minecraft.world.SimpleContainer(18);
            
            // Fila 1: Mostrar la oferta
            // Slot 3: Item ofrecido
            container.setItem(3, selectedOffer.getOfferedItem().copy());
            
            // Slot 4: Flecha (indica intercambio)
            ItemStack arrow = new ItemStack(Items.ARROW);
            arrow.setHoverName(Component.literal("§7Intercambio"));
            container.setItem(4, arrow);
            
            // Slot 5: Item solicitado
            container.setItem(5, selectedOffer.getRequestedItem().copy());
            
            // Fila 2: Botones de confirmación
            // Slot 10: Confirmar SÍ (bloque verde)
            ItemStack confirmButton = new ItemStack(Items.LIME_CONCRETE);
            confirmButton.setHoverName(Component.literal(config.get("buttons.confirm_yes")));
            container.setItem(10, confirmButton);
            
            // Slot 16: Cancelar NO (bloque rojo)
            ItemStack cancelButton = new ItemStack(Items.RED_CONCRETE);
            cancelButton.setHoverName(Component.literal(config.get("buttons.confirm_no")));
            container.setItem(16, cancelButton);
            
            // Abrir el menú
            player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (windowId, playerInventory, p) -> new BarterChestMenu(
                    MenuType.GENERIC_9x2, 
                    windowId, 
                    playerInventory, 
                    container, 
                    2, 
                    player,
                    "confirm_delete"
                ),
                Component.literal(config.get("menu.confirm_delete_title"))
            ));
            
        } catch (Exception e) {
            LoggerUtil.error("Error opening delete confirmation menu: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Elimina una oferta y devuelve el item al jugador
     */
    private void deleteOffer(ServerPlayer player) {
        try {
            MessageConfig config = MessageConfig.getInstance();
            
            // Obtener la oferta guardada
            com.barterhouse.api.TradeOffer offer = com.barterhouse.event.SignEditHandler.getSelectedOffer(player.getUUID());
            
            if (offer == null) {
                player.displayClientMessage(Component.literal(config.get("errors.offer_not_found")), true);
                BarterUIManager.openDeleteMyOffersGUI(player);
                return;
            }
            
            // Verificar que el jugador es el dueño de la oferta
            if (!offer.getCreatorUUID().equals(player.getUUID())) {
                player.displayClientMessage(Component.literal(config.get("errors.not_owner")), true);
                BarterUIManager.openDeleteMyOffersGUI(player);
                return;
            }
            
            // Devolver el item ofrecido a la BODEGA del jugador (NO al inventario)
            ItemStack offeredItem = offer.getOfferedItem().copy();
            com.barterhouse.manager.WarehouseManager.getInstance().addItem(
                player.getUUID(),
                offeredItem,
                "Sistema" // El sistema devuelve el item
            );
            
            // Eliminar la oferta del sistema
            com.barterhouse.manager.TradeOfferManager.getInstance().removeOffer(offer.getOfferId());
            
            // Limpiar la oferta guardada
            com.barterhouse.event.SignEditHandler.clearSelectedOffer(player.getUUID());
            
            // Mensaje de confirmación
            player.sendSystemMessage(Component.literal(config.get("success.offer_deleted")));
            player.sendSystemMessage(Component.literal(config.get("success.item_returned_to_warehouse", "count", offeredItem.getCount(), "item", offeredItem.getDisplayName().getString())));
            
            LoggerUtil.info("Offer " + offer.getOfferId() + " deleted by player " + player.getName().getString());
            
            // Volver al menú de eliminar
            BarterUIManager.openDeleteMyOffersGUI(player);
            
        } catch (Exception e) {
            LoggerUtil.error("Error deleting offer: " + e.getMessage());
            e.printStackTrace();
            MessageConfig configErr = MessageConfig.getInstance();
            player.displayClientMessage(Component.literal(configErr.get("errors.delete_error")), true);
        }
    }
    
    /**
     * Retira un item de la bodega y lo da al jugador
     */
    private void withdrawFromWarehouse(ServerPlayer player, int slotId) {
        try {
            MessageConfig config = MessageConfig.getInstance();
            
            // Obtener items de la bodega
            java.util.List<com.barterhouse.manager.WarehouseManager.StoredItem> warehouseItems = 
                com.barterhouse.manager.WarehouseManager.getInstance().getPlayerWarehouse(player.getUUID());
            
            if (slotId >= warehouseItems.size()) {
                player.displayClientMessage(Component.literal(config.get("errors.warehouse_item_not_found")), true);
                BarterUIManager.openWarehouseGUI(player);
                return;
            }
            
            // Obtener el item
            com.barterhouse.manager.WarehouseManager.StoredItem storedItem = warehouseItems.get(slotId);
            
            // Crear el ItemStack
            net.minecraft.world.item.Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS
                .getValue(new net.minecraft.resources.ResourceLocation(storedItem.itemName));
            
            if (item == null) {
                player.displayClientMessage(Component.literal(config.get("errors.warehouse_item_error")), true);
                BarterUIManager.openWarehouseGUI(player);
                return;
            }
            
            ItemStack itemStack = new ItemStack(item, storedItem.count);
            
            // Dar el item al jugador
            boolean added = player.addItem(itemStack);
            
            if (!added) {
                // Si el inventario está lleno, dropear el item
                player.drop(itemStack, false);
                player.sendSystemMessage(Component.literal(config.get("success.inventory_full")));
            }
            
            // Remover el item de la bodega
            com.barterhouse.manager.WarehouseManager.getInstance().removeItem(player.getUUID(), slotId);
            
            // Mensaje de confirmación
            player.sendSystemMessage(Component.literal(config.get("success.warehouse_withdrawn", "count", storedItem.count, "item", item.getDescription().getString())));
            
            LoggerUtil.info("Player " + player.getName().getString() + " withdrew " + storedItem.count + "x " + storedItem.itemName + " from warehouse");
            
            // Volver a la bodega
            BarterUIManager.openWarehouseGUI(player);
            
        } catch (Exception e) {
            LoggerUtil.error("Error withdrawing from warehouse: " + e.getMessage());
            e.printStackTrace();
            MessageConfig configErr = MessageConfig.getInstance();
            player.displayClientMessage(Component.literal(configErr.get("errors.warehouse_error")), true);
        }
    }
}