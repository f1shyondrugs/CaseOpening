package com.f1shy312.caseOpening.models;

import com.f1shy312.caseOpening.main;
import com.f1shy312.caseOpening.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import net.milkbowl.vault.economy.Economy;
import de.tr7zw.nbtapi.NBTContainer;
import de.tr7zw.nbtapi.NBTItem;

import java.util.List;
import java.util.Random;
import java.util.HashMap;

public class Reward {
    private final double chance;
    private final String type;
    private final String material;
    private final List<String> commands;
    private final String displayItem;
    private final String displayName;
    private final int minAmount;
    private final int maxAmount;
    private final Random random;
    private final String nbtData;
    private final main plugin;

    public Reward(double chance, String type, String material, List<String> commands, 
                 String displayItem, String displayName, int minAmount, int maxAmount, 
                 String nbtData, main plugin) {
        this.chance = chance;
        this.type = type;
        this.material = material;
        this.commands = commands;
        this.displayItem = displayItem;
        this.displayName = displayName;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.random = new Random();
        this.nbtData = nbtData;
        this.plugin = plugin;
    }

    public void give(Player player) {
        try {
            switch (type.toUpperCase()) {
                case "ITEM":
                    ItemStack item = new ItemStack(Material.valueOf(material));
                    item.setAmount(getRandomAmount());
                    
                    if (nbtData != null && !nbtData.isEmpty()) {
                        try {
                            Class.forName("de.tr7zw.nbtapi.NBTItem");
                            NBTContainer nbtContainer = new NBTContainer(nbtData);
                            NBTItem nbtItem = new NBTItem(item);
                            nbtItem.mergeCompound(nbtContainer);
                            item = nbtItem.getItem();
                        } catch (ClassNotFoundException e) {
                            plugin.getLogger().warning("NBTAPI not found - NBT data will not be applied");
                        } catch (Exception e) {
                            Bukkit.getLogger().warning("Failed to apply NBT data to reward: " + e.getMessage());
                        }
                    }
                    
                    HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                    if (!leftover.isEmpty()) {
                        for (ItemStack drop : leftover.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), drop);
                        }
                    }
                    break;

                case "COMMAND":
                    if (commands != null && !commands.isEmpty()) {
                        for (String cmd : commands) {
                            String finalCmd = cmd.replace("%player%", player.getName());
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
                        }
                    }
                    break;

                case "MONEY":
                    RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
                    if (rsp != null) {
                        Economy econ = rsp.getProvider();
                        double amount = getRandomAmount();
                        econ.depositPlayer(player, amount);
                    } else {
                        throw new RuntimeException("Economy provider not found!");
                    }
                    break;

                default:
                    throw new IllegalStateException("Unknown reward type: " + type);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to give reward: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-throw to handle in GUI
        }
    }

    public double getChance() {
        return chance;
    }

    public String getType() {
        return type;
    }

    public String getMaterial() {
        return material;
    }

    public List<String> getCommands() {
        return commands;
    }

    public String getDisplayItem() {
        return displayItem != null ? displayItem : "STONE";
    }

    public String getDisplayName() {
        return displayName != null ? displayName : "&7Unknown Reward";
    }

    public int getMinAmount() {
        return minAmount;
    }

    public int getMaxAmount() {
        return maxAmount;
    }

    public String getNbtData() {
        return nbtData;
    }

    private int getRandomAmount() {
        if (minAmount == maxAmount) {
            return minAmount;
        }
        return minAmount + random.nextInt(maxAmount - minAmount + 1);
    }
} 
