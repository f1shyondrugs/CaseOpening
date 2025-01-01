package com.f1shy312.caseOpening.managers;

import com.f1shy312.caseOpening.main;
import com.f1shy312.caseOpening.utils.ColorUtils;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HologramManager {
    private final main plugin;
    private final Map<Location, String> holograms;
    private int hologramCounter = 0;

    public HologramManager(main plugin) {
        this.plugin = plugin;
        this.holograms = new HashMap<>();
    }

    public void createHologram(Location location, String crateId) {
        removeHologram(location);
        
        ConfigurationSection crateSection = plugin.getCrateManager().getCrateConfig(crateId);
        if (crateSection == null) {
            return;
        }

        List<String> lines = new ArrayList<>();
        
        ConfigurationSection hologramSection = crateSection.getConfigurationSection("hologram");
        if (hologramSection != null) {
            String crateName = hologramSection.getString("name", crateSection.getString("display-name", crateId));
            
            List<String> crateLines = hologramSection.getStringList("lines");
            if (!crateLines.isEmpty()) {
                lines.addAll(crateLines);
            } else {
                List<String> defaultLines = plugin.getConfig().getStringList("hologram.lines");
                for (String line : defaultLines) {
                    lines.add(line.replace("%crate_name%", crateName));
                }
            }
        }

        String holoName = "crate_" + location.getWorld().getName() + "_" + 
            location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ();
        
        Hologram existing = DHAPI.getHologram(holoName);
        if (existing != null) {
            existing.delete();
        }
        
        Location holoLoc = location.clone().add(0.5, 
            plugin.getConfig().getDouble("hologram.offset", 2.5), 0.5);
        
        lines = lines.stream()
            .map(ColorUtils::colorize)
            .collect(Collectors.toList());

        DHAPI.createHologram(holoName, holoLoc, lines);
        
        holograms.put(location, holoName);
    }

    public void removeHologram(Location location) {
        String holoName = holograms.remove(location);
        if (holoName != null) {
            Hologram hologram = DHAPI.getHologram(holoName);
            if (hologram != null) {
                hologram.delete();
            }
        }
    }

    public void removeAllHolograms() {
        for (String holoName : holograms.values()) {
            Hologram hologram = DHAPI.getHologram(holoName);
            if (hologram != null) {
                hologram.delete();
            }
        }
        holograms.clear();
    }
} 