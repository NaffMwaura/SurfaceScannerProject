package com.thinpixel;

import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * SurfaceScanner - The Java Bridge for Thin-Pixel Scanning.
 * This plugin captures exact sub-block coordinates for high-resolution 3D data.
 */
public class SurfaceScanner extends JavaPlugin implements Listener {

    private final List<ScanPoint> sessionData = new ArrayList<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("Thin-Pixel Surface Scanner initialized. Use a Diamond Hoe to scan.");
    }

    @EventHandler
    public void onScan(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        // Using Diamond Hoe as the 'Scanner Tool'
        if (event.getItem() != null && event.getItem().getType() == Material.DIAMOND_HOE) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                performHighResScan(player);
            } else if (event.getAction() == Action.LEFT_CLICK_AIR) {
                saveScanData(player);
            }
        }
    }

    private void performHighResScan(Player player) {
        // Raytrace up to 100 blocks to find the exact hit position on a surface
        RayTraceResult result = player.getWorld().rayTraceBlocks(
            player.getEyeLocation(), 
            player.getEyeLocation().getDirection(), 
            100.0, 
            FluidCollisionMode.NEVER
        );

        if (result != null && result.getHitBlock() != null) {
            Vector hitPos = result.getHitPosition();
            Material blockType = result.getHitBlock().getType();
            
            ScanPoint point = new ScanPoint(
                hitPos.getX(), 
                hitPos.getY(), 
                hitPos.getZ(), 
                blockType.name()
            );
            
            sessionData.add(point);
            player.sendMessage("§b[Scanner] §fCaptured point: §7" + String.format("%.4f, %.4f, %.4f", point.x, point.y, point.z));
        }
    }

    private void saveScanData(Player player) {
        if (sessionData.isEmpty()) {
            player.sendMessage("§c[Scanner] No data to save!");
            return;
        }

        File dataFile = new File(getDataFolder(), "raw_scan.json");
        if (!getDataFolder().exists()) getDataFolder().mkdirs();

        try (FileWriter writer = new FileWriter(dataFile)) {
            // Simple CSV-like format for the Python engine to read easily
            writer.write("x,y,z,material\n");
            for (ScanPoint p : sessionData) {
                writer.write(p.x + "," + p.y + "," + p.z + "," + p.material + "\n");
            }
            player.sendMessage("§a[Scanner] Data saved to /plugins/SurfaceScanner/raw_scan.json");
            sessionData.clear();
        } catch (IOException e) {
            player.sendMessage("§c[Scanner] Error saving data.");
            e.printStackTrace();
        }
    }

    // Data structure for the scan points
    static class ScanPoint {
        double x, y, z;
        String material;

        ScanPoint(double x, double y, double z, String material) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.material = material;
        }
    }
}