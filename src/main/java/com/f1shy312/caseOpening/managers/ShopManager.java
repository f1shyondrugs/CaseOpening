package com.f1shy312.caseOpening.managers;

import com.f1shy312.caseOpening.main;
import com.f1shy312.caseOpening.models.ShopItem;
import com.f1shy312.caseOpening.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopManager {
    private final main plugin;
    private final Map<String, ShopItem> shopItems;
    private int currentGuiSize = 27;

    public ShopManager(main plugin) {
        this.plugin = plugin;
        this.shopItems = new HashMap<>();
        loadShopItems();
    }

    public void loadShopItems() {
        shopItems.clear();
        plugin.getLogger().info("Loading shop items...");
        
        File casesFile = new File(plugin.getDataFolder(), "cases.yml");
        if (!casesFile.exists()) {
            plugin.getLogger().warning("cases.yml not found!");
            return;
        }

        FileConfiguration casesConfig = YamlConfiguration.loadConfiguration(casesFile);
        ConfigurationSection cratesSection = casesConfig.getConfigurationSection("crates");
        if (cratesSection == null) {
            plugin.getLogger().warning("No crates section found in cases.yml!");
            return;
        }

        List<String> crateIds = new ArrayList<>(cratesSection.getKeys(false));
        plugin.getLogger().info("Found " + crateIds.size() + " crates: " + String.join(", ", crateIds));
        int numCrates = crateIds.size();
        
        currentGuiSize = 27;
        if (numCrates > 7) currentGuiSize = 45;
        
        List<Integer> slots = calculateCenterSlots(currentGuiSize, numCrates);

        for (int i = 0; i < crateIds.size(); i++) {
            String crateId = crateIds.get(i);
            ConfigurationSection crateSection = cratesSection.getConfigurationSection(crateId);
            
            if (crateSection == null) {
                plugin.getLogger().warning("Could not load crate section for " + crateId);
                continue;
            }

            ConfigurationSection keySection = crateSection.getConfigurationSection("key");
            if (keySection == null) {
                plugin.getLogger().warning("No key section found for crate: " + crateId);
                continue;
            }

            String displayName = keySection.getString("name", crateId + " Key");
            double price = keySection.getDouble("price", 1000.0);
            
            ConfigurationSection shopSection = keySection.getConfigurationSection("shop");
            int amount = shopSection != null ? shopSection.getInt("amount", 1) : 1;
            int slot = shopSection != null ? shopSection.getInt("slot", slots.get(i)) : slots.get(i);
            
            List<String> lore = new ArrayList<>(keySection.getStringList("lore"));
            if (shopSection != null) {
                List<String> additionalLore = shopSection.getStringList("additional_lore");
                if (!additionalLore.isEmpty()) {
                    if (!lore.isEmpty()) lore.add("");
                    lore.addAll(additionalLore);
                }
            }

            Material material = Material.valueOf(keySection.getString("material", "TRIPWIRE_HOOK"));
            
            plugin.getLogger().info("Loading shop item: " + crateId + " (display: " + displayName + ") at slot " + slot + " with price " + price);

            shopItems.put(crateId, new ShopItem(
                crateId,
                displayName,
                material,
                price,
                amount,
                slot,
                lore
            ));
        }
        
        plugin.getLogger().info("Loaded " + shopItems.size() + " shop items");
    }

    private List<Integer> calculateCenterSlots(int guiSize, int numItems) {
        List<Integer> slots = new ArrayList<>();
        int rows = guiSize / 9;
        int middleRow = rows / 2;
        
        int maxItemsPerRow = Math.min(7, numItems);
        int startSlot = (9 - maxItemsPerRow) / 2;
        
        if (numItems <= 7) {
            for (int i = 0; i < numItems; i++) {
                slots.add((middleRow * 9) + startSlot + i);
            }
        } else {
            int itemsFirstRow = (numItems + 1) / 2;
            int itemsSecondRow = numItems / 2;
            
            startSlot = (9 - itemsFirstRow) / 2;
            for (int i = 0; i < itemsFirstRow; i++) {
                slots.add((middleRow * 9) + startSlot + i);
            }
            
            startSlot = (9 - itemsSecondRow) / 2;
            for (int i = 0; i < itemsSecondRow; i++) {
                slots.add(((middleRow + 1) * 9) + startSlot + i);
            }
        }
        
        return slots;
    }

    public void openShop(Player player) {
        String title = ColorUtils.colorize(plugin.getConfig().getString("shop.settings.title", "Key Shop"));
        Inventory inv = Bukkit.createInventory(null, currentGuiSize, title);

        if (plugin.getConfig().getBoolean("shop.settings.fill-empty", true)) {
            ItemStack filler = createFillerItem(plugin.getConfig().getString("shop.settings.filler-material", "BLACK_STAINED_GLASS_PANE"));
            for (int i = 0; i < currentGuiSize; i++) {
                inv.setItem(i, filler);
            }
        }

        for (ShopItem item : shopItems.values()) {
            if (item.getSlot() >= 0 && item.getSlot() < currentGuiSize) {
                inv.setItem(item.getSlot(), createShopItemStack(item));
            }
        }

        player.openInventory(inv);
    }

    private ItemStack createFillerItem(String material) {
        ItemStack item = new ItemStack(Material.valueOf(material));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createShopItemStack(ShopItem item) {
        ItemStack itemStack = new ItemStack(item.getMaterial());
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize(item.getDisplayName()));
            
            List<String> lore = new ArrayList<>();
            if (!item.getLore().isEmpty()) {
                item.getLore().forEach(line -> lore.add(ColorUtils.colorize(line)));
                lore.add("");
            }
            
            lore.add(ColorUtils.colorize("&7Price: &a$" + item.getPrice()));
            lore.add(ColorUtils.colorize("&7Amount: &e" + item.getAmount()));
            lore.add("");
            lore.add(ColorUtils.colorize("&eClick to purchase!"));
            
            meta.setLore(lore);
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    public ShopItem getShopItem(int slot) {
        return shopItems.values().stream()
                .filter(item -> item.getSlot() == slot)
                .findFirst()
                .orElse(null);
    }
} 