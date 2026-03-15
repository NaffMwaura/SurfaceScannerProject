package com.thinpixel;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Universal Surface Scanner
 * Compatible with Minecraft Java 1.7.2 - 1.21.1
 * Captures: Terrain, Motion, Inventory, and Held Items.
 */
public class SurfaceScanner extends JavaPlugin implements Listener {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("SurfaceScanner (Multi-Version) enabled!");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Use legacy-compatible method for 1.7.2 - 1.8 support
        ItemStack itemInHand = event.getPlayer().getItemInHand();
        
        if (itemInHand == null) return;

        // Trigger scan when right-clicking with a STICK
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
            if (itemInHand.getType() == Material.STICK) {
                scanSurface(event.getPlayer());
            }
        }
    }

    private void scanSurface(Player player) {
        player.sendMessage("§a[Scanner] Initializing 0.0001 surface thinning...");
        
        // Capture Main-Thread data (Motion & Inventory) before going async
        Vector velocity = player.getVelocity();
        String heldItem = player.getItemInHand().getType().toString();
        
        // Capture Inventory contents
        List<String> inventoryItems = new ArrayList<>();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                inventoryItems.add(item.getType().toString());
            }
        }

        
        int radius = 20; 
        int centerX = player.getLocation().getBlockX();
        int centerZ = player.getLocation().getBlockZ();
        org.bukkit.World world = player.getWorld();

        // Run the heavy terrain calculation asynchronously
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            Map<String, Object> exportData = new HashMap<>();
            
            // 1. Add Metadata (Motion, Inventory, Held Item)
            Map<String, Object> playerState = new HashMap<>();
            playerState.put("motion_x", velocity.getX());
            playerState.put("motion_y", velocity.getY());
            playerState.put("motion_z", velocity.getZ());
            playerState.put("held_block", heldItem);
            playerState.put("inventory", inventoryItems);
            playerState.put("player_name", player.getName());
            
            exportData.put("player_state", playerState);

            // 2. Add Terrain Scan
            List<Map<String, Object>> terrainData = new ArrayList<>();
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    // Safe for most versions; world object remains valid
                    Block b = world.getHighestBlockAt(centerX + x, centerZ + z);
                    
                    Map<String, Object> blockInfo = new HashMap<>();
                    blockInfo.put("x", b.getX());
                    blockInfo.put("y", b.getY());
                    blockInfo.put("z", b.getZ());
                    blockInfo.put("type", b.getType().toString());
                    
                    terrainData.add(blockInfo);
                }
            }
            exportData.put("terrain", terrainData);

            // Save to JSON
            saveToJson(exportData, player);
        });
    }

    private void saveToJson(Map<String, Object> data, Player player) {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        File file = new File(getDataFolder(), "surface_data.json");

        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(data, writer);
            
            getServer().getScheduler().runTask(this, () -> {
                player.sendMessage("§6[Scanner] SUCCESS: Data exported with motion & inventory.");
                player.sendMessage("§fFile: /plugins/SurfaceScanner/surface_data.json");
            });
            
        } catch (IOException e) {
            getServer().getScheduler().runTask(this, () -> {
                player.sendMessage("§c[Scanner] Error: Failed to write JSON file.");
            });
            e.printStackTrace();
        }
    }
}