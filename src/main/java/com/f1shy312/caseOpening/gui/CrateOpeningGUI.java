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
        
        String title = ColorUtils.colorize(plugin.getConfig().getString("gui.opening.title", "&8Opening %crate_name%")
            .replace("%crate_name%", crate.getDisplayName()));
        this.inventory = Bukkit.createInventory(this, 27, title);
        
        // Set filler items
        ItemStack filler = new ItemStack(Material.valueOf(
            plugin.getConfig().getString("gui.opening.filler-material", "BLACK_STAINED_GLASS_PANE")));
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(ColorUtils.colorize(
            plugin.getConfig().getString("gui.opening.filler-name", " ")));
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
        isAnimating = true;
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
                Bukkit.getScheduler().cancelTask(taskId);
                taskId = -1;
            } catch (Exception e) {
                plugin.getLogger().warning("Error cancelling animation task: " + e.getMessage());
            }
        }
        isAnimating = false;
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
            // Use configured message from config.yml
            String message = plugin.getConfig().getString("messages.reward-won", "&aYou won: &e%reward_name%&a!")
                .replace("%reward_name%", reward.getDisplayName());
            player.sendMessage(ColorUtils.formatMessage(plugin, message));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            
            // Immediately clear the player's opening state
            if (plugin.getCrateInteractListener() != null) {
                plugin.getCrateInteractListener().clearAllPlayerStates(player);
            }
            setPlayerOpening(player.getUniqueId(), false);
            
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
        return isAnimating && taskId != -1;
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