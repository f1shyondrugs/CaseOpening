package com.f1shy312.caseOpening.gui;

import com.f1shy312.caseOpening.main;
import com.f1shy312.caseOpening.models.Crate;
import com.f1shy312.caseOpening.models.Reward;
import com.f1shy312.caseOpening.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class CrateOpeningGUI implements InventoryHolder {
    private final main plugin;
    private final Player player;
    private final Crate crate;
    private final Inventory inventory;
    private final Random random;
    private int taskId = -1;
    private int ticks = 0;
    private static final int[] DISPLAY_SLOTS = {10, 11, 12, 13, 14, 15, 16};
    private static final int SELECTOR_SLOT = 13;
    private static final int ANIMATION_LENGTH = 55;
    private static final long START_DELAY = 1L;
    private static final long END_DELAY = 20L;
    private static final int FINAL_SLOWDOWN = 45;
    private long currentDelay = START_DELAY;
    private final List<Reward> displayedRewards;
    private boolean isAnimating = false;
    private static final java.util.Set<UUID> activeOpenings = new java.util.HashSet<>();

    public CrateOpeningGUI(main plugin, Player player, Crate crate) {
        this.plugin = plugin;
        this.player = player;
        this.crate = crate;
        this.random = new Random();
        this.displayedRewards = new ArrayList<>();
        
        // Initialize display rewards
        for (int i = 0; i < DISPLAY_SLOTS.length + 13; i++) {
            displayedRewards.add(crate.getRandomReward());
        }
        
        this.inventory = Bukkit.createInventory(this, 27, ColorUtils.colorize("Opening " + crate.getDisplayName()));
        
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);
        
        for (int i = 0; i < inventory.getSize(); i++) {
            if (!isDisplaySlot(i)) {
                inventory.setItem(i, filler);
            }
        }
    }

    private boolean isDisplaySlot(int slot) {
        for (int displaySlot : DISPLAY_SLOTS) {
            if (displaySlot == slot) return true;
        }
        return false;
    }

    public void open() {
        if (isPlayerOpening(player.getUniqueId())) {
            player.sendMessage(ColorUtils.formatMessage(plugin, plugin.getConfig().getString("messages.already-opening", "&cYou are already opening a case!")));
            return;
        }
        setPlayerOpening(player.getUniqueId(), true);
        player.openInventory(inventory);
        startAnimation();
    }

    private void startAnimation() {
        if (taskId != -1) {
            stopAnimation();
        }

        isAnimating = true;
        taskId = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.getOpenInventory().getTopInventory().equals(inventory)) {
                    stopAnimation();
                    return;
                }

                if (ticks >= ANIMATION_LENGTH) {
                    isAnimating = false;
                    stopAnimation();
                    // Let the final displayed reward be the one we give
                    Reward finalReward = displayedRewards.get(DISPLAY_SLOTS.length / 2);
                    giveReward(finalReward);
                    return;
                }

                try {
                    if (ticks < ANIMATION_LENGTH) {
                        displayedRewards.remove(0);
                        displayedRewards.add(crate.getRandomReward());
                    }

                    for (int i = 0; i < DISPLAY_SLOTS.length; i++) {
                        Reward reward = displayedRewards.get(i);
                        displayReward(DISPLAY_SLOTS[i], reward);
                    }

                    float progress = (float) ticks / ANIMATION_LENGTH;
                    float easedProgress = progress * progress * progress;
                    currentDelay = START_DELAY + (long)(easedProgress * (END_DELAY - START_DELAY));
                    
                    ticks++;
                    
                    int currentTaskId = taskId;
                    cancel();
                    
                    if (taskId == currentTaskId && taskId != -1) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (taskId == currentTaskId && taskId != -1) {
                                    startAnimation();
                                }
                            }
                        }.runTaskLater(plugin, currentDelay);
                    }

                    float pitch = Math.max(0.7f, 1.2f - progress);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.3f, pitch);

                } catch (Exception e) {
                    plugin.getLogger().severe("Error during crate animation: " + e.getMessage());
                    stopAnimation();
                    player.closeInventory();
                }
            }
        }.runTaskTimer(plugin, 0L, currentDelay).getTaskId();
    }

    public void stopAnimation() {
        if (taskId != -1) {
            try {
                int oldTaskId = taskId;
                taskId = -1;
                Bukkit.getScheduler().cancelTask(oldTaskId);
            } catch (Exception e) {
                plugin.getLogger().warning("Error cancelling animation task: " + e.getMessage());
            }
        }
        setPlayerOpening(player.getUniqueId(), false);
    }

    private void displayReward(int slot, Reward reward) {
        try {
            ItemStack display = new ItemStack(Material.valueOf(reward.getDisplayItem()));
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ColorUtils.colorize(reward.getDisplayName()));
                display.setItemMeta(meta);
                inventory.setItem(slot, display);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error displaying reward: " + e.getMessage());
        }
    }

    private void giveReward(Reward reward) {
        try {
            reward.give(player);
            player.sendMessage(ColorUtils.formatMessage(plugin, "%prefix%&aYou won: &e" + reward.getDisplayName() + "&a!"));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            
            // Close inventory after showing the message
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.closeInventory();
            }, 20L);
        } catch (Exception e) {
            plugin.getLogger().severe("Error giving reward: " + e.getMessage());
            player.sendMessage(ColorUtils.colorize("&cAn error occurred while giving your reward."));
            player.closeInventory();
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public boolean isAnimating() {
        return isAnimating;
    }

    public static boolean isPlayerOpening(UUID playerId) {
        return activeOpenings.contains(playerId);
    }

    public static void setPlayerOpening(UUID playerId, boolean opening) {
        if (opening) {
            activeOpenings.add(playerId);
        } else {
            activeOpenings.remove(playerId);
        }
    }
} 