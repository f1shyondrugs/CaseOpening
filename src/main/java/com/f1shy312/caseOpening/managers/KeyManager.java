package com.f1shy312.caseOpening.managers;

import com.f1shy312.caseOpening.main;
import com.f1shy312.caseOpening.models.Crate;
import com.f1shy312.caseOpening.utils.ColorUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class KeyManager {
    private final main plugin;
    private final File keysFile;
    private FileConfiguration keysConfig;

    public KeyManager(main plugin) {
        this.plugin = plugin;
        this.keysFile = new File(plugin.getDataFolder(), "keys.yml");
        loadKeys();
    }

    public void loadKeys() {
        if (!keysFile.exists()) {
            plugin.saveResource("keys.yml", false);
        }
        keysConfig = YamlConfiguration.loadConfiguration(keysFile);
    }

    public void saveKeys() {
        try {
            keysConfig.save(keysFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save keys.yml!");
            e.printStackTrace();
        }
    }

    public int getKeyCount(Player player, String keyType) {
        return keysConfig.getInt("players." + player.getUniqueId() + "." + keyType.toLowerCase(), 0);
    }

    public void setKeyCount(Player player, String keyType, int amount) {
        keysConfig.set("players." + player.getUniqueId() + "." + keyType.toLowerCase(), amount);
        saveKeys();
    }

    public void giveKey(Player player, String keyType, int amount) {
        int current = getKeyCount(player, keyType);
        setKeyCount(player, keyType, current + amount);
        
        String keyName = plugin.getCrateManager().getCrate(keyType).getDisplayName();
        String message = plugin.getConfig().getString("messages.key-received");
        player.sendMessage(ColorUtils.formatMessage(plugin, message)
            .replace("%amount%", String.valueOf(amount))
            .replace("%key_name%", keyName));
    }

    public boolean hasKey(Player player, String keyType) {
        return getKeyCount(player, keyType) > 0;
    }

    public boolean useKey(Player player, String keyType) {
        int current = getKeyCount(player, keyType);
        if (current <= 0) return false;
        
        setKeyCount(player, keyType, current - 1);
        return true;
    }

    public ItemStack createPhysicalKey(String keyType, int amount) {
        Crate crate = plugin.getCrateManager().getCrate(keyType);
        if (crate == null) return null;

        File casesFile = new File(plugin.getDataFolder(), "cases.yml");
        FileConfiguration casesConfig = YamlConfiguration.loadConfiguration(casesFile);
        
        ConfigurationSection keyConfig = casesConfig
            .getConfigurationSection("crates." + keyType + ".key");
        if (keyConfig == null) {
            plugin.getLogger().warning("No key configuration found for crate type: " + keyType);
            return null;
        }

        ItemStack key = new ItemStack(Material.valueOf(keyConfig.getString("material", "TRIPWIRE_HOOK")), amount);
        ItemMeta meta = key.getItemMeta();
        
        meta.setDisplayName(ColorUtils.colorize(keyConfig.getString("name", "&fKey")));
        List<String> lore = keyConfig.getStringList("lore")
            .stream()
            .map(ColorUtils::colorize)
            .collect(Collectors.toList());
        meta.setLore(lore);
        
        meta.getPersistentDataContainer().set(
            new NamespacedKey(plugin, "crate_key"),
            PersistentDataType.STRING,
            keyType.toLowerCase()
        );
        
        key.setItemMeta(meta);
        return key;
    }

    public void givePhysicalKey(Player player, String crateId, int amount) {
        ConfigurationSection crateSection = plugin.getCrateManager().getCrateConfig(crateId);
        if (crateSection == null) {
            plugin.getLogger().warning("Could not find crate configuration for: " + crateId);
            return;
        }

        ConfigurationSection keySection = crateSection.getConfigurationSection("key");
        if (keySection == null) {
            plugin.getLogger().warning("No key configuration found for crate: " + crateId);
            return;
        }

        ItemStack keyItem = new ItemStack(Material.valueOf(keySection.getString("material", "TRIPWIRE_HOOK")));
        ItemMeta meta = keyItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize(keySection.getString("name", "&fKey for " + crateId)));
            
            List<String> lore = keySection.getStringList("lore");
            lore = lore.stream()
                .map(ColorUtils::colorize)
                .collect(Collectors.toList());
            meta.setLore(lore);

            meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "crate_key"),
                PersistentDataType.STRING,
                crateId
            );

            keyItem.setItemMeta(meta);
        }

        keyItem.setAmount(amount);
        
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(keyItem);
        if (!leftover.isEmpty()) {
            for (ItemStack item : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
        }

        String keyName = keySection.getString("name", crateId + " Key");
        player.sendMessage(ColorUtils.formatMessage(plugin, 
            plugin.getConfig().getString("messages.key-received")
                .replace("%amount%", String.valueOf(amount))
                .replace("%key_name%", keyName)
        ));
    }
}