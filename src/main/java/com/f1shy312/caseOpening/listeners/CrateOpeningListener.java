package com.f1shy312.caseOpening.listeners;

import com.f1shy312.caseOpening.main;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.entity.Player;

public class CrateOpeningListener implements Listener {
    private final main plugin;

    public CrateOpeningListener(main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        if (event.getView().getTitle().startsWith("Opening ")) {
            if (!player.hasPermission("caseopening.crate.open")) {
                player.sendMessage(ColorUtils.formatMessage(plugin, plugin.getConfig().getString("messages.no-permission")));
                player.closeInventory();
                return;
            }
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().startsWith("Opening ")) {
            // TODO: Handle early inventory close
        }
    }
} 