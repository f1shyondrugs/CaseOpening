package com.f1shy312.caseOpening.listeners;

import com.f1shy312.caseOpening.main;
import com.f1shy312.caseOpening.models.Crate;
import com.f1shy312.caseOpening.gui.CrateOpeningGUI;
import com.f1shy312.caseOpening.models.Reward;
import com.f1shy312.caseOpening.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.Inventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;

import java.util.ArrayList;
import java.util.List;

public class CrateInteractListener implements Listener {
    private final main plugin;

    public CrateInteractListener(main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCrateInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (player.hasMetadata("placing_crate")) {
            if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
            event.setCancelled(true);
            
            Block block = event.getClickedBlock();
            if (block == null) return;
            
            String crateId = player.getMetadata("placing_crate").get(0).asString();
            Crate crate = plugin.getCrateManager().getCrate(crateId);
            if (crate == null) {
                player.sendMessage(ColorUtils.formatMessage(plugin, "%prefix%&cInvalid crate type!"));
                return;
            }

            Material crateType = Material.valueOf(crate.getBlockType());
            
            block.setType(crateType);
            if (block.getBlockData() instanceof Directional) {
                Directional directional = (Directional) block.getBlockData();
                directional.setFacing(getPlayerFacing(player));
                block.setBlockData(directional);
            }
            
            plugin.getCrateManager().placeCrate(block.getLocation(), crateId, getPlayerFacing(player));
            plugin.getHologramManager().createHologram(block.getLocation(), crateId);
            
            player.removeMetadata("placing_crate", plugin);
            player.sendMessage(ColorUtils.formatMessage(plugin, plugin.getConfig().getString("messages.crate-placed")));
            return;
        }

        if (event.getClickedBlock() == null) {
            return;
        }

        Crate crate = plugin.getCrateManager().getCrateAtLocation(event.getClickedBlock().getLocation());
        if (crate == null) {
            return;
        }

        event.setCancelled(true);

        if (event.getAction() == Action.LEFT_CLICK_BLOCK && player.isSneaking()) {
            if (player.hasPermission("caseopening.crate.destroy")) {
                Location location = event.getClickedBlock().getLocation();
                plugin.getCrateManager().removeCrate(location);
                event.getClickedBlock().setType(org.bukkit.Material.AIR);
                player.sendMessage(ColorUtils.formatMessage(plugin, 
                    plugin.getConfig().getString("messages.crate-destroyed")));
            } else {
                player.sendMessage(ColorUtils.formatMessage(plugin, 
                    plugin.getConfig().getString("messages.no-permission")));
            }
            return;
        }

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (!player.hasPermission("caseopening.crate.preview")) {
                player.sendMessage(ColorUtils.formatMessage(plugin, plugin.getConfig().getString("messages.no-permission")));
                return;
            }
            showCrateContents(player, crate);
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (!player.hasPermission("caseopening.crate.open")) {
                player.sendMessage(ColorUtils.formatMessage(plugin, plugin.getConfig().getString("messages.no-permission")));
                return;
            }
            ItemStack item = event.getItem();

            if (item == null || !item.hasItemMeta()) {
                player.sendMessage(ColorUtils.formatMessage(plugin, plugin.getConfig().getString("messages.no-key")));
                return;
            }

            NamespacedKey keyTypeKey = new NamespacedKey(plugin, "crate_key");
            String keyType = item.getItemMeta().getPersistentDataContainer().get(keyTypeKey, PersistentDataType.STRING);

            if (keyType == null || !keyType.equals(crate.getId().toLowerCase())) {
                player.sendMessage(ColorUtils.formatMessage(plugin, plugin.getConfig().getString("messages.wrong-key")));
                return;
            }

            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().setItem(event.getHand(), null);
            }

            new CrateOpeningGUI(plugin, player, crate).open();
        }
    }

    private void showCrateContents(Player player, Crate crate) {
        List<Reward> rewards = crate.getRewards();
        
        int rows = 3;
        int size = rows * 9;
        
        Inventory inv = Bukkit.createInventory(null, size, 
            ColorUtils.colorize("&8" + crate.getDisplayName() + " &8Contents"));

        ItemStack blackPane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemStack yellowPane = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
        ItemMeta paneMeta = blackPane.getItemMeta();
        if (paneMeta != null) {
            paneMeta.setDisplayName(" ");
            blackPane.setItemMeta(paneMeta);
            yellowPane.setItemMeta(paneMeta);
        }
        
        for (int i = 0; i < size; i++) {
            inv.setItem(i, blackPane);
        }

        int numRewards = Math.min(rewards.size(), 7);
        int startSlot = 10;
        
        for (int i = 10; i <= 16; i++) {
            inv.setItem(i, yellowPane);
        }
        
        inv.setItem(9, blackPane);
        inv.setItem(17, blackPane);

        for (int i = 0; i < numRewards; i++) {
            Reward reward = rewards.get(i);
            try {
                ItemStack display = new ItemStack(Material.valueOf(reward.getDisplayItem()));
                ItemMeta meta = display.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ColorUtils.colorize(reward.getDisplayName()));
                    
                    List<String> lore = new ArrayList<>();
                    lore.add("");
                    lore.add(ColorUtils.colorize("&7Chance: &e" + String.format("%.1f", reward.getChance()) + "%"));
                    
                    if (reward.getMinAmount() != reward.getMaxAmount()) {
                        lore.add(ColorUtils.colorize("&7Amount: &e" + reward.getMinAmount() + "-" + reward.getMaxAmount()));
                    } else if (reward.getMinAmount() > 1) {
                        lore.add(ColorUtils.colorize("&7Amount: &e" + reward.getMinAmount()));
                    }
                    
                    switch (reward.getType().toUpperCase()) {
                        case "MONEY":
                            lore.add(ColorUtils.colorize("&7Type: &a&lMoney Reward"));
                            break;
                        case "COMMAND":
                            lore.add(ColorUtils.colorize("&7Type: &d&lSpecial Reward"));
                            break;
                        case "ITEM":
                            lore.add(ColorUtils.colorize("&7Type: &b&lItem Reward"));
                            break;
                    }
                    
                    meta.setLore(lore);
                    display.setItemMeta(meta);
                    
                    inv.setItem(startSlot + i, display);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material for reward display: " + reward.getDisplayItem());
            }
        }

        if (rewards.size() > 7) {
            ItemStack moreInfo = new ItemStack(Material.BOOK);
            ItemMeta moreInfoMeta = moreInfo.getItemMeta();
            if (moreInfoMeta != null) {
                moreInfoMeta.setDisplayName(ColorUtils.colorize("&e&lMore Rewards..."));
                List<String> lore = new ArrayList<>();
                lore.add(ColorUtils.colorize("&7There are &e" + (rewards.size() - 7) + " &7more rewards"));
                lore.add(ColorUtils.colorize("&7that are not shown here."));
                moreInfoMeta.setLore(lore);
                moreInfo.setItemMeta(moreInfoMeta);
                inv.setItem(17, moreInfo);
            }
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onCrateBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Crate crate = plugin.getCrateManager().getCrateAtLocation(block.getLocation());
        
        if (crate != null) {
            Player player = event.getPlayer();
            if (player.isSneaking()) {
                if (!player.hasPermission("caseopening.crate.destroy")) {
                    event.setCancelled(true);
                    player.sendMessage(ColorUtils.formatMessage(plugin, plugin.getConfig().getString("messages.no-permission")));
                    return;
                }
                
                plugin.getCrateManager().removeCrate(block.getLocation());
                plugin.getHologramManager().removeHologram(block.getLocation());
                player.sendMessage(ColorUtils.formatMessage(plugin, plugin.getConfig().getString("messages.crate-removed")));
            } else {
                event.setCancelled(true);
                player.sendMessage(ColorUtils.formatMessage(plugin, "&cYou must be sneaking to destroy crates!"));
            }
        }
    }

    private BlockFace getPlayerFacing(Player player) {
        float yaw = player.getLocation().getYaw();
        if (yaw < 0) {
            yaw += 360;
        }
        if (yaw >= 315 || yaw < 45) {
            return BlockFace.NORTH;
        } else if (yaw < 135) {
            return BlockFace.EAST;
        } else if (yaw < 225) {
            return BlockFace.SOUTH;
        } else if (yaw < 315) {
            return BlockFace.WEST;
        }
        return BlockFace.NORTH;
    }
}