package com.f1shy312.caseOpening.models;

import com.f1shy312.caseOpening.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import net.milkbowl.vault.economy.Economy;

import java.util.List;
import java.util.Random;

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

    public Reward(double chance, String type, String material, List<String> commands, 
                 String displayItem, String displayName, int minAmount, int maxAmount) {
        this.chance = chance;
        this.type = type;
        this.material = material;
        this.commands = commands;
        this.displayItem = displayItem;
        this.displayName = displayName;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.random = new Random();
    }

    public void give(Player player) {
        try {
            switch (type.toUpperCase()) {
                case "ITEM":
                    ItemStack item = new ItemStack(Material.valueOf(material), minAmount);
                    player.getInventory().addItem(item);
                    break;
                case "COMMAND":
                    if (commands != null && !commands.isEmpty()) {
                        for (String cmd : commands) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                                cmd.replace("%player%", player.getName()));
                        }
                    }
                    break;
                case "MONEY":
                    RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
                    if (rsp != null) {
                        Economy econ = rsp.getProvider();
                        double amount = minAmount;
                        if (maxAmount > minAmount) {
                            amount = minAmount + (Math.random() * (maxAmount - minAmount));
                        }
                        econ.depositPlayer(player, amount);
                    } else {
                        throw new RuntimeException("Economy provider not found!");
                    }
                    break;
                default:
                    throw new IllegalStateException("Unknown reward type: " + type);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to give reward: " + e.getMessage(), e);
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
} 
