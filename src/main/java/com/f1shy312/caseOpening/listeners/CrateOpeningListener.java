package com.f1shy312.caseOpening.listeners;

import com.f1shy312.caseOpening.main;
import com.f1shy312.caseOpening.utils.ColorUtils;
import com.f1shy312.caseOpening.gui.CrateOpeningGUI;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.entity.Player;

public class CrateOpeningListener implements Listener {
    private final main plugin;

    public CrateOpeningListener(main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        if (event.getView().getTitle().startsWith(ColorUtils.colorize(
                plugin.getConfig().getString("gui.opening.title", "&8Opening").split("%")[0]))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        if (event.getView().getTitle().startsWith(ColorUtils.colorize(
                plugin.getConfig().getString("gui.opening.title", "&8Opening").split("%")[0]))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        
        if (event.getView().getTitle().startsWith(ColorUtils.colorize(
                plugin.getConfig().getString("gui.opening.title", "&8Opening").split("%")[0]))) {
            
            if (event.getInventory().getHolder() instanceof CrateOpeningGUI gui && gui.isAnimating()) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.openInventory(event.getInventory());
                });
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (player.getOpenInventory() != null && 
            player.getOpenInventory().getTopInventory().getHolder() instanceof CrateOpeningGUI gui) {
            gui.stopAnimation();
        }
    }
} 