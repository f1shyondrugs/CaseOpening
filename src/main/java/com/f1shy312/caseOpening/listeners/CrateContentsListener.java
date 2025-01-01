package com.f1shy312.caseOpening.listeners;

import com.f1shy312.caseOpening.main;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import com.f1shy312.caseOpening.utils.ColorUtils;

public class CrateContentsListener implements Listener {
    private final main plugin;

    public CrateContentsListener(main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        String title = event.getView().getTitle();
        if (title.contains("Contents") && title.startsWith("§8")) {
            if (!player.hasPermission("caseopening.crate.preview")) {
                player.sendMessage(ColorUtils.formatMessage(plugin, plugin.getConfig().getString("messages.no-permission")));
                player.closeInventory();
                return;
            }
            event.setCancelled(true);
            
            if (event.getWhoClicked() instanceof Player player) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            }
        }
    }
} 