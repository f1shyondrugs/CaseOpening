package com.f1shy312.caseOpening.listeners;

import com.f1shy312.caseOpening.main;
import com.f1shy312.caseOpening.models.ShopItem;
import com.f1shy312.caseOpening.utils.ColorUtils;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.RegisteredServiceProvider;

public class GUIListener implements Listener {
    private final main plugin;
    private final Economy econ;

    public GUIListener(main plugin) {
        this.plugin = plugin;
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().severe("No economy provider found!");
            this.econ = null;
            return;
        }
        this.econ = rsp.getProvider();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String shopTitle = ColorUtils.colorize(plugin.getConfig().getString("shop.settings.title", "Key Shop"));
        
        if (event.getView().getTitle().equals(shopTitle)) {
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory())) {
                event.setCancelled(true);
                
                if (!(event.getWhoClicked() instanceof Player player)) return;
                if (event.getCurrentItem() == null) return;
                
                ShopItem item = plugin.getShopManager().getShopItem(event.getSlot());
                if (item == null) return;

                if (plugin.getCrateManager().getCrate(item.getId()) == null) {
                    player.sendMessage(ColorUtils.formatMessage(plugin, "&cThis key is no longer available!"));
                    return;
                }

                if (econ == null) {
                    player.sendMessage(ColorUtils.formatMessage(plugin, "&cEconomy system is not available!"));
                    return;
                }

                double price = item.getPrice();
                if (!econ.has(player, price)) {
                    player.sendMessage(ColorUtils.formatMessage(plugin, plugin.getConfig().getString("messages.insufficient-funds", "&cYou don't have enough money!")));
                    return;
                }

                econ.withdrawPlayer(player, price);
                plugin.getKeyManager().givePhysicalKey(player, item.getId(), item.getAmount());
                
                plugin.getLogManager().logKeyPurchase(
                    player.getName(),
                    item.getId(),
                    item.getAmount(),
                    price
                );

                player.sendMessage(ColorUtils.formatMessage(plugin, plugin.getConfig().getString("messages.purchase-success", "&aSuccessfully purchased %amount% %key_name% key(s)!")
                        .replace("%amount%", String.valueOf(item.getAmount()))
                        .replace("%key_name%", item.getDisplayName())));
            }
        }
    }
} 