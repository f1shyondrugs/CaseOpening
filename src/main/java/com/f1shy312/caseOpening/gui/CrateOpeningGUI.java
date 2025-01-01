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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CrateOpeningGUI {
    private final main plugin;
    private final Player player;
    private final Crate crate;
    private final Inventory inventory;
    private final Random random;
    private int taskId = -1;
    private int ticks = 0;
    private final Reward finalReward;
    private static final int[] DISPLAY_SLOTS = {10, 11, 12, 13, 14, 15, 16};
    private static final int SELECTOR_SLOT = 13;
    private static final int ANIMATION_LENGTH = 45;
    private static final long START_DELAY = 1L;
    private static final long END_DELAY = 20L;
    private static final int FINAL_SLOWDOWN = 35;
    private long currentDelay = START_DELAY;
    private List<Reward> displayedRewards;

    public CrateOpeningGUI(main plugin, Player player, Crate crate) {
        this.plugin = plugin;
        this.player = player;
        this.crate = crate;
        this.random = new Random();
        this.finalReward = crate.getRandomReward();
        this.displayedRewards = new ArrayList<>();
        
        for (int i = 0; i < DISPLAY_SLOTS.length + 3; i++) {
            displayedRewards.add(crate.getRandomReward());
        }
        
        this.inventory = Bukkit.createInventory(null, 27, ColorUtils.colorize("Opening " + crate.getDisplayName()));
        
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
        player.openInventory(inventory);
        startAnimation();
    }

    private void startAnimation() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }

        taskId = new BukkitRunnable() {
            private int offset = 0;

            @Override
            public void run() {
                if (ticks >= ANIMATION_LENGTH) {
                    stopAnimation();
                    giveReward();
                    return;
                }

                if (ticks < FINAL_SLOWDOWN) {
                    displayedRewards.remove(0);
                    displayedRewards.add(crate.getRandomReward());
                }

                for (int i = 0; i < DISPLAY_SLOTS.length; i++) {
                    try {
                        Reward reward = displayedRewards.get(i);
                        displayReward(DISPLAY_SLOTS[i], reward);
                    } catch (Exception e) {
                        plugin.getLogger().severe("Error during animation: " + e.getMessage());
                        stopAnimation();
                        player.closeInventory();
                        return;
                    }
                }

                float progress = (float) ticks / ANIMATION_LENGTH;
                float easedProgress = progress * progress * progress;
                currentDelay = START_DELAY + (long)(easedProgress * (END_DELAY - START_DELAY));
                
                cancel();
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (taskId != -1) {
                            startAnimation();
                        }
                    }
                }.runTaskLater(plugin, currentDelay);

                float pitch = Math.max(0.7f, 1.2f - progress);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.3f, pitch);

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, currentDelay).getTaskId();
    }

    private void stopAnimation() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;

            for (int i = 0; i < DISPLAY_SLOTS.length; i++) {
                displayReward(DISPLAY_SLOTS[i], finalReward);
            }
            
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
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

    private void giveReward() {
        try {
            player.closeInventory();
            if (finalReward != null) {
                finalReward.give(player);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            } else {
                plugin.getLogger().warning("Final reward was null for player " + player.getName());
                player.sendMessage(ColorUtils.colorize("&cError: Could not determine reward!"));
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error giving reward: " + e.getMessage());
            player.sendMessage(ColorUtils.colorize("&cAn error occurred while giving your reward."));
        }
    }

    private void giveReward(Reward reward) {
        reward.give(player);
        
        player.sendMessage(ColorUtils.formatMessage(plugin, "%prefix%&aYou won: &e" + reward.getDisplayName() + "&a!"));
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.closeInventory();
        }, 20L);
    }
} 