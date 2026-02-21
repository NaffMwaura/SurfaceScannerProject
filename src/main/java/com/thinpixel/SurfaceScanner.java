package com.thinpixel;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SurfaceScanner extends JavaPlugin implements Listener {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public void onEnable() {
        // Register the listener so the Stick interaction works
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("SurfaceScanner enabled!");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Trigger scan when right-clicking with a STICK
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
            if (event.getPlayer().getInventory().getItemInMainHand().getType() == Material.STICK) {
                scanSurface(event.getPlayer());
            }
        }
    }

    private void scanSurface(Player player) {
        player.sendMessage("§aStarting surface scan in background...");

        // Capture necessary data from the main thread before going async
        // (You cannot call certain Bukkit methods safely from an async thread, 
        // but world.getHighestBlockAt is generally safe in modern Paper versions)
        int radius = 25; // Increased radius since it's now async
        int centerX = player.getLocation().getBlockX();
        int centerZ = player.getLocation().getBlockZ();
        org.bukkit.World world = player.getWorld();

        // Start the background task
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            List<Map<String, Object>> scanData = new ArrayList<>();

            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    // Get the highest block at these coordinates
                    Block b = world.getHighestBlockAt(centerX + x, centerZ + z);
                    
                    Map<String, Object> blockInfo = new HashMap<>();
                    blockInfo.put("x", b.getX());
                    blockInfo.put("y", b.getY());
                    blockInfo.put("z", b.getZ());
                    blockInfo.put("type", b.getType().toString());
                    
                    scanData.add(blockInfo);
                }
            }

            // Save the data to file (still on the background thread)
            saveToJson(scanData, player);
        });
    }

    private void saveToJson(List<Map<String, Object>> data, Player player) {
        // Ensure plugin folder exists
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        File file = new File(getDataFolder(), "surface_data.json");

        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(data, writer);
            
            // Use a task to send the message back on the main thread
            getServer().getScheduler().runTask(this, () -> {
                player.sendMessage("§6Scan complete! Saved " + data.size() + " blocks to: §f/plugins/SurfaceScanner/" + file.getName());
            });
            
            getLogger().info("SurfaceScanner: Saved data for " + player.getName());
        } catch (IOException e) {
            getServer().getScheduler().runTask(this, () -> {
                player.sendMessage("§cFailed to save scan data.");
            });
            e.printStackTrace();
        }
    }
}