package com.f1shy312.caseOpening.managers;

import com.f1shy312.caseOpening.main;
import com.f1shy312.caseOpening.models.Crate;
import com.f1shy312.caseOpening.models.Reward;
import com.f1shy312.caseOpening.utils.ColorUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.Directional;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import de.tr7zw.nbtapi.NBTContainer;
import de.tr7zw.nbtapi.NBTItem;

public class CrateManager {
    private final main plugin;
    private final Map<String, Crate> crates;
    private final Map<Location, String> crateLocations;
    private final File locationFile;
    private final FileConfiguration locationConfig;
    private FileConfiguration casesConfig;

    public CrateManager(main plugin, File casesFile) {
        this.plugin = plugin;
        this.crates = new HashMap<>();
        this.crateLocations = new HashMap<>();
        this.locationFile = new File(plugin.getDataFolder(), "locations.yml");
        this.locationConfig = YamlConfiguration.loadConfiguration(locationFile);
        this.casesConfig = YamlConfiguration.loadConfiguration(casesFile);
        
        loadCrates();
        loadLocations();
    }

    private void loadCrates() {
        ConfigurationSection cratesSection = casesConfig.getConfigurationSection("crates");
        if (cratesSection == null) {
            plugin.getLogger().severe("No crates section found in config!");
            return;
        }

        for (String crateId : cratesSection.getKeys(false)) {
            ConfigurationSection crateSection = cratesSection.getConfigurationSection(crateId);
            if (crateSection == null) continue;

            Crate crate = new Crate(
                plugin,
                crateId,
                crateSection.getString("display-name"),
                crateSection.getString("block-type")
            );
            
            List<Map<?, ?>> rewardsList = crateSection.getMapList("rewards");
            if (rewardsList.isEmpty()) {
                plugin.getLogger().warning("No rewards found for crate: " + crateId);
                continue;
            }

            for (Map<?, ?> rewardMap : rewardsList) {
                try {
                    double chance = ((Number) rewardMap.get("chance")).doubleValue();
                    String type = String.valueOf(rewardMap.get("type"));
                    String material = String.valueOf(rewardMap.get("material"));
                    
                    List<String> commands = new ArrayList<>();
                    Object commandsObj = rewardMap.get("commands");
                    if (commandsObj instanceof List<?>) {
                        for (Object cmd : (List<?>) commandsObj) {
                            commands.add(String.valueOf(cmd));
                        }
                    }

                    String nbtData = null;
                    if (rewardMap.containsKey("nbt")) {
                        nbtData = String.valueOf(rewardMap.get("nbt"));
                    }

                    String displayItem = rewardMap.containsKey("display-item") ? 
                        String.valueOf(rewardMap.get("display-item")) : material;
                    String displayName = rewardMap.containsKey("display-name") ? 
                        String.valueOf(rewardMap.get("display-name")) : material;

                    int minAmount = 1;
                    int maxAmount = 1;
                    Object amountObj = rewardMap.get("amount");
                    if (amountObj != null) {
                        String amountStr = String.valueOf(amountObj);
                        if (amountStr.contains("-")) {
                            String[] parts = amountStr.split("-");
                            minAmount = Integer.parseInt(parts[0]);
                            maxAmount = Integer.parseInt(parts[1]);
                        } else {
                            minAmount = maxAmount = Integer.parseInt(amountStr);
                        }
                    }

                    Reward reward = new Reward(
                        chance,
                        type,
                        material,
                        commands,
                        displayItem,
                        displayName,
                        minAmount,
                        maxAmount,
                        nbtData,
                        plugin
                    );
                    crate.addReward(reward);
                    plugin.getLogger().info("Loaded reward " + reward.getDisplayName() + " for crate " + crateId);
                } catch (Exception e) {
                    plugin.getLogger().severe("Error loading reward for crate " + crateId + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            crates.put(crateId, crate);
            plugin.getLogger().info("Loaded crate " + crateId + " with " + crate.getRewardCount() + " rewards");
        }
    }

    public void saveCrateLocations() {
        locationConfig.set("locations", null);
        int counter = 0;

        for (Map.Entry<Location, String> entry : crateLocations.entrySet()) {
            Location loc = entry.getKey();
            String crateId = entry.getValue();

            String key = "locations." + counter;
            locationConfig.set(key + ".world", loc.getWorld().getName());
            locationConfig.set(key + ".x", loc.getX());
            locationConfig.set(key + ".y", loc.getY());
            locationConfig.set(key + ".z", loc.getZ());
            locationConfig.set(key + ".yaw", loc.getYaw());
            locationConfig.set(key + ".crate-id", crateId);

            if (loc.getBlock().getBlockData() instanceof Directional) {
                Directional directional = (Directional) loc.getBlock().getBlockData();
                locationConfig.set(key + ".facing", directional.getFacing().name());
            }

            counter++;
        }

        try {
            locationConfig.save(locationFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save locations.yml!");
            e.printStackTrace();
        }
    }

    private void loadLocations() {
        if (!locationFile.exists()) {
            try {
                locationFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create locations.yml!");
                return;
            }
        }

        ConfigurationSection locationsSection = locationConfig.getConfigurationSection("locations");
        if (locationsSection == null) {
            plugin.getLogger().info("No crate locations found to load.");
            return;
        }

        plugin.getLogger().info("Loading crate locations...");
        
        List<Map<String, Object>> pendingHolograms = new ArrayList<>();

        for (String key : locationsSection.getKeys(false)) {
            ConfigurationSection locationSection = locationsSection.getConfigurationSection(key);
            if (locationSection == null) continue;

            try {
                String world = locationSection.getString("world");
                double x = locationSection.getDouble("x");
                double y = locationSection.getDouble("y");
                double z = locationSection.getDouble("z");
                float yaw = (float) locationSection.getDouble("yaw", 0.0);
                String crateId = locationSection.getString("crate-id");

                if (world == null || crateId == null) {
                    plugin.getLogger().warning("Invalid location data for key: " + key);
                    continue;
                }

                if (plugin.getServer().getWorld(world) == null) {
                    plugin.getLogger().warning("World not found: " + world);
                    continue;
                }

                Location location = new Location(plugin.getServer().getWorld(world), x, y, z);
                location.setYaw(yaw);

                Crate crate = getCrate(crateId);
                if (crate != null) {
                    crateLocations.put(location, crateId);

                    location.getBlock().setType(Material.valueOf(crate.getBlockType()));

                    if (location.getBlock().getBlockData() instanceof Directional) {
                        Directional directional = (Directional) location.getBlock().getBlockData();
                        String facing = locationSection.getString("facing", "NORTH");
                        directional.setFacing(BlockFace.valueOf(facing));
                        location.getBlock().setBlockData(directional);
                    }

                    Map<String, Object> holoData = new HashMap<>();
                    holoData.put("location", location);
                    holoData.put("crateId", crateId);
                    pendingHolograms.add(holoData);
                    
                    plugin.getLogger().info("Restored crate " + crateId + " at " + 
                        String.format("%.1f,%.1f,%.1f in %s", x, y, z, world));
                } else {
                    plugin.getLogger().warning("Could not find crate type: " + crateId);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error loading crate location for key " + key + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getLogger().info("Creating holograms for " + pendingHolograms.size() + " crates...");
            for (Map<String, Object> holoData : pendingHolograms) {
                Location loc = (Location) holoData.get("location");
                String crateId = (String) holoData.get("crateId");
                plugin.getHologramManager().createHologram(loc, crateId);
            }
            plugin.getLogger().info("Finished creating holograms!");
        }, 40L);
        
        plugin.getLogger().info("Loaded " + crateLocations.size() + " crate locations.");
    }

    public void placeCrate(Location location, String crateId, BlockFace facing) {
        crateLocations.put(location, crateId);
        
        if (location.getBlock().getBlockData() instanceof Directional) {
            Directional directional = (Directional) location.getBlock().getBlockData();
            directional.setFacing(facing);
            location.getBlock().setBlockData(directional);
        }
        
        saveCrateLocations(location, crateId, facing);
        plugin.getHologramManager().createHologram(location, crateId);
    }

    private void saveCrateLocations(Location loc, String crateId, BlockFace facing) {
        locationConfig.set("locations", null);
        int counter = 0;

        String key = "locations." + counter;
        locationConfig.set(key + ".world", loc.getWorld().getName());
        locationConfig.set(key + ".x", loc.getX());
        locationConfig.set(key + ".y", loc.getY());
        locationConfig.set(key + ".z", loc.getZ());
        locationConfig.set(key + ".yaw", loc.getYaw());
        locationConfig.set(key + ".crate-id", crateId);
        locationConfig.set(key + ".facing", facing.name());
        counter++;

        for (Map.Entry<Location, String> entry : crateLocations.entrySet()) {
            if (entry.getKey().equals(loc)) continue;
            
            Location location = entry.getKey();
            String id = entry.getValue();
            
            key = "locations." + counter;
            locationConfig.set(key + ".world", location.getWorld().getName());
            locationConfig.set(key + ".x", location.getX());
            locationConfig.set(key + ".y", location.getY());
            locationConfig.set(key + ".z", location.getZ());
            locationConfig.set(key + ".yaw", location.getYaw());
            locationConfig.set(key + ".crate-id", id);
            
            if (location.getBlock().getBlockData() instanceof Directional) {
                Directional directional = (Directional) location.getBlock().getBlockData();
                locationConfig.set(key + ".facing", directional.getFacing().name());
            }
            
            counter++;
        }

        try {
            locationConfig.save(locationFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save locations.yml!");
            e.printStackTrace();
        }
    }

    public void removeCrate(Location location) {
        crateLocations.remove(location);
        plugin.getHologramManager().removeHologram(location);
        saveCrateLocations();
    }

    public Crate getCrate(String id) {
        return crates.get(id);
    }

    public Crate getCrateAtLocation(Location location) {
        String crateId = crateLocations.get(location);
        if (crateId == null) return null;
        return crates.get(crateId);
    }

    public void reloadCrates() {
        plugin.getLogger().info("Reloading crates...");
        
        Map<Location, String> oldLocations = new HashMap<>(crateLocations);
        
        plugin.getHologramManager().removeAllHolograms();
        crateLocations.clear();
        crates.clear();
        
        File casesFile = new File(plugin.getDataFolder(), "cases.yml");
        if (!casesFile.exists()) {
            plugin.saveResource("cases.yml", false);
        }
        
        casesConfig = YamlConfiguration.loadConfiguration(casesFile);
        
        loadCrates();
        loadLocations();
        
        plugin.getLogger().info("Reload complete! Loaded:");
        plugin.getLogger().info("- " + crates.size() + " crate types");
        plugin.getLogger().info("- " + crateLocations.size() + " placed crates");
    }

    public ConfigurationSection getCrateConfig(String crateId) {
        File casesFile = new File(plugin.getDataFolder(), "cases.yml");
        if (!casesFile.exists()) return null;
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(casesFile);
        return config.getConfigurationSection("crates." + crateId);
    }
}