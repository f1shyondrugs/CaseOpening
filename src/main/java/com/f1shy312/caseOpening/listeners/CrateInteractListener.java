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
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.NamespacedKey;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class CrateInteractListener implements Listener {
    private final main plugin;
    private final Map<UUID, Long> lastInteractionTime = new HashMap<>();
    private static final long INTERACTION_COOLDOWN = 500; // 500ms cooldown
    private final Set<UUID> playersOpeningCrates = new HashSet<>();
    private final Object lock = new Object();
    private static final long REOPEN_DELAY = 20L; // 1 second delay before allowing reopening
    private final Set<UUID> playersInCooldown = new HashSet<>();
    private static final long COOLDOWN_DURATION = 60L; // 3 second cooldown

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
            event.setCancelled(true);
            
            synchronized (lock) {
                if (!player.hasPermission("caseopening.crate.open")) {
                    player.sendMessage(ColorUtils.formatMessage(plugin, plugin.getConfig().getString("messages.no-permission")));
                    return;
                }
                
                if (playersOpeningCrates.contains(player.getUniqueId()) || playersInCooldown.contains(player.getUniqueId())) {
                    player.sendMessage(ColorUtils.formatMessage(plugin, plugin.getConfig().getString("messages.already-opening")));
                    return;
                }

                playersOpeningCrates.add(player.getUniqueId());
                playersInCooldown.add(player.getUniqueId());
                
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    synchronized (lock) {
                        playersInCooldown.remove(player.getUniqueId());
                    }
                }, COOLDOWN_DURATION);
            }
            
            try {
                ItemStack item = event.getItem();
                
                if (item == null || !item.hasItemMeta()) {
                    player.sendMessage(ColorUtils.formatMessage(plugin, plugin.getConfig().getString("messages.no-key")));
                    return;
                }

                NamespacedKey keyTypeKey = new NamespacedKey(plugin, "crate_key");
                PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
                
                if (!container.has(keyTypeKey, PersistentDataType.STRING)) {
                    player.sendMessage(ColorUtils.formatMessage(plugin, plugin.getConfig().getString("messages.no-key")));
                    return;
                }

                String keyType = container.get(keyTypeKey, PersistentDataType.STRING);
                
                if (keyType == null || !keyType.equals(crate.getId().toLowerCase())) {
                    player.sendMessage(ColorUtils.formatMessage(plugin, plugin.getConfig().getString("messages.wrong-key")));
                    return;
                }

                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    player.getInventory().setItemInMainHand(null);
                }
                
            } catch (Exception e) {
                synchronized (lock) {
                    playersOpeningCrates.remove(player.getUniqueId());
                }
                plugin.getLogger().warning("Error processing crate key: " + e.getMessage());
                return;
            }

            try {
                new CrateOpeningGUI(plugin, player, crate) {
                    @Override
                    public void onClose() {
                        super.onClose();
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            synchronized (lock) {
                                playersOpeningCrates.remove(player.getUniqueId());
                                plugin.getLogger().info("Player " + player.getName() + " finished opening crate");
                                
                                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                    if (!playersOpeningCrates.contains(player.getUniqueId())) {
                                        plugin.getLogger().info("Player " + player.getName() + " can now open crates again");
                                    }
                                }, REOPEN_DELAY);
                            }
                        }, 10L);
                    }
                }.open();
            } catch (Exception e) {
                synchronized (lock) {
                    playersOpeningCrates.remove(player.getUniqueId());
                }
                plugin.getLogger().warning("Error during crate opening: " + e.getMessage());
                player.sendMessage(ColorUtils.formatMessage(plugin, "&cAn error occurred while opening the crate."));
            }
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

    public void clearPlayerOpeningState(Player player) {
        playersOpeningCrates.remove(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        synchronized (lock) {
            playersOpeningCrates.remove(event.getPlayer().getUniqueId());
            playersInCooldown.remove(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        
        if (event.getView().getTitle().startsWith("Opening ")) {
            synchronized (lock) {
                if (!playersInCooldown.contains(player.getUniqueId())) {
                    playersInCooldown.add(player.getUniqueId());
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        synchronized (lock) {
                            playersInCooldown.remove(player.getUniqueId());
                            playersOpeningCrates.remove(player.getUniqueId());
                        }
                    }, COOLDOWN_DURATION);
                }
            }
        }
    }

    public void clearAllPlayerStates(Player player) {
        synchronized (lock) {
            playersOpeningCrates.remove(player.getUniqueId());
            playersInCooldown.remove(player.getUniqueId());
        }
    }
}