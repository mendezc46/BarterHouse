package com.barterhouse.manager;

import com.barterhouse.util.LoggerUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Gestor de bodega - Almacena los items que los jugadores han recibido de transacciones
 */
public class WarehouseManager {
    
    private static WarehouseManager instance;
    private Path warehouseFile;
    private final Map<UUID, List<StoredItem>> playerWarehouses = new HashMap<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    /**
     * Clase para almacenar items en la bodega
     */
    public static class StoredItem {
        public String itemName;
        public int count;
        public long receivedTime;
        public String sourcePlayer; // Nombre del jugador que realizó la transacción
        public String nbtData; // NBT del item (para items con datos especiales)
        
        public StoredItem(ItemStack stack, String sourcePlayer) {
            this.itemName = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
            this.count = stack.getCount();
            this.receivedTime = System.currentTimeMillis();
            this.sourcePlayer = sourcePlayer;
            
            // Guardar NBT si el item tiene datos
            if (stack.hasTag()) {
                this.nbtData = stack.getTag().getAsString();
            }
        }
        
        public StoredItem() {} // Para GSON
    }
    
    /**
     * Obtiene la instancia única del manager
     */
    public static synchronized WarehouseManager getInstance() {
        if (instance == null) {
            instance = new WarehouseManager();
        }
        return instance;
    }
    
    /**
     * Inicializa el manager con el nivel del servidor
     */
    public void initializeWithLevel(Level level) {
        if (this.warehouseFile == null && level != null) {
            try {
                Path worldPath = level.getServer().getServerDirectory().toPath();
                Path barterhouseDir = worldPath.resolve("barterhouse");
                Files.createDirectories(barterhouseDir);
                
                this.warehouseFile = barterhouseDir.resolve("warehouse.json");
                
                // Cargar la bodega si existe
                if (Files.exists(warehouseFile)) {
                    loadWarehouse();
                    LoggerUtil.info("Warehouse loaded from: " + warehouseFile.toAbsolutePath());
                } else {
                    LoggerUtil.info("New warehouse created at: " + warehouseFile.toAbsolutePath());
                }
            } catch (Exception e) {
                LoggerUtil.error("Error initializing warehouse: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Agrega un item a la bodega del jugador
     */
    public void addItem(UUID playerUUID, ItemStack stack, String sourcePlayerName) {
        try {
            List<StoredItem> warehouse = playerWarehouses.computeIfAbsent(playerUUID, k -> new ArrayList<>());
            
            // Buscar si ya existe el mismo item para apilarlo
            for (StoredItem stored : warehouse) {
                if (stored.itemName.equals(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).toString()) &&
                    Objects.equals(stored.nbtData, (stack.hasTag() ? stack.getTag().getAsString() : null))) {
                    stored.count += stack.getCount();
                    saveWarehouse();
                    LoggerUtil.info("Added " + stack.getCount() + "x " + stack.getDisplayName().getString() + 
                                   " to warehouse of " + playerUUID);
                    return;
                }
            }
            
            // Si no existe, crear nuevo
            warehouse.add(new StoredItem(stack, sourcePlayerName));
            saveWarehouse();
            LoggerUtil.info("Added " + stack.getCount() + "x " + stack.getDisplayName().getString() + 
                           " to warehouse of " + playerUUID);
            
        } catch (Exception e) {
            LoggerUtil.error("Error adding item to warehouse: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Obtiene la bodega de un jugador
     */
    public List<StoredItem> getPlayerWarehouse(UUID playerUUID) {
        return playerWarehouses.getOrDefault(playerUUID, new ArrayList<>());
    }
    
    /**
     * Elimina un item de la bodega
     */
    public void removeItem(UUID playerUUID, int index) {
        try {
            List<StoredItem> warehouse = playerWarehouses.get(playerUUID);
            if (warehouse != null && index >= 0 && index < warehouse.size()) {
                warehouse.remove(index);
                saveWarehouse();
                LoggerUtil.info("Removed item from warehouse of " + playerUUID);
            }
        } catch (Exception e) {
            LoggerUtil.error("Error removing item from warehouse: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Reduce la cantidad de un item en la bodega
     */
    public void reduceItem(UUID playerUUID, int index, int amount) {
        try {
            List<StoredItem> warehouse = playerWarehouses.get(playerUUID);
            if (warehouse != null && index >= 0 && index < warehouse.size()) {
                StoredItem item = warehouse.get(index);
                item.count -= amount;
                if (item.count <= 0) {
                    warehouse.remove(index);
                }
                saveWarehouse();
                LoggerUtil.info("Reduced item in warehouse of " + playerUUID);
            }
        } catch (Exception e) {
            LoggerUtil.error("Error reducing item in warehouse: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Guarda la bodega en archivo JSON
     */
    private void saveWarehouse() {
        if (warehouseFile == null) return;
        
        try {
            JsonObject root = new JsonObject();
            
            for (Map.Entry<UUID, List<StoredItem>> entry : playerWarehouses.entrySet()) {
                JsonArray itemsArray = new JsonArray();
                for (StoredItem item : entry.getValue()) {
                    itemsArray.add(gson.toJsonTree(item));
                }
                root.add(entry.getKey().toString(), itemsArray);
            }
            
            Files.write(warehouseFile, gson.toJson(root).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            LoggerUtil.error("Error saving warehouse: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Carga la bodega desde archivo JSON
     */
    private void loadWarehouse() {
        if (warehouseFile == null || !Files.exists(warehouseFile)) {
            return;
        }
        
        try {
            String content = Files.readString(warehouseFile, StandardCharsets.UTF_8);
            JsonObject root = gson.fromJson(content, JsonObject.class);
            
            if (root == null) return;
            
            for (String uuidStr : root.keySet()) {
                try {
                    UUID playerUUID = UUID.fromString(uuidStr);
                    JsonArray itemsArray = root.getAsJsonArray(uuidStr);
                    List<StoredItem> warehouse = new ArrayList<>();
                    
                    for (int i = 0; i < itemsArray.size(); i++) {
                        StoredItem item = gson.fromJson(itemsArray.get(i), StoredItem.class);
                        warehouse.add(item);
                    }
                    
                    playerWarehouses.put(playerUUID, warehouse);
                } catch (IllegalArgumentException e) {
                    LoggerUtil.error("Invalid UUID in warehouse file: " + uuidStr);
                }
            }
            
            LoggerUtil.info("Loaded warehouse data for " + playerWarehouses.size() + " players");
        } catch (Exception e) {
            LoggerUtil.error("Error loading warehouse: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Obtiene el total de items en la bodega de un jugador
     */
    public int getTotalItems(UUID playerUUID) {
        List<StoredItem> warehouse = getPlayerWarehouse(playerUUID);
        return warehouse.stream().mapToInt(item -> item.count).sum();
    }
}
