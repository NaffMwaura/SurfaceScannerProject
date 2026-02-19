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

    @Override
    public void onEnable() {
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
        player.sendMessage("§aStarting surface scan...");
        
        List<Map<String, Object>> scanData = new ArrayList<>();
        int radius = 10;
        Block center = player.getLocation().getBlock();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                Block b = player.getWorld().getHighestBlockAt(center.getX() + x, center.getZ() + z);
                
                Map<String, Object> blockInfo = new HashMap<>();
                // Use .put() for Maps, not .add()
                blockInfo.put("x", b.getX());
                blockInfo.put("y", b.getY());
                blockInfo.put("z", b.getZ());
                blockInfo.put("type", b.getType().toString());
                
                scanData.add(blockInfo);
            }
        }

        saveToJson(scanData, player);
    }

    private void saveToJson(List<Map<String, Object>> data, Player player) {
        // Ensure plugin folder exists
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        File file = new File(getDataFolder(), "surface_data.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(data, writer);
            player.sendMessage("§6Scan complete! Saved to: §f/plugins/SurfaceScanner/" + file.getName());
        } catch (IOException e) {
            player.sendMessage("§cFailed to save scan data.");
            e.printStackTrace();
        }
    }
}