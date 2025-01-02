package com.f1shy312.caseOpening.models;

import com.f1shy312.caseOpening.main;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Crate {
    private final main plugin;
    private final String id;
    private final String displayName;
    private final String blockType;
    private final List<Reward> rewards;
    private final Random random;

    public Crate(main plugin, String id, String displayName, String blockType) {
        this.plugin = plugin;
        this.id = id;
        this.displayName = displayName;
        this.blockType = blockType;
        this.rewards = new ArrayList<>();
        this.random = new Random();
    }

    public void addReward(Reward reward) {
        rewards.add(reward);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBlockType() {
        return blockType;
    }

    public int getRewardCount() {
        return rewards.size();
    }

    public Reward getRandomReward() {
        if (rewards.isEmpty()) {
            return null;
        }

        double totalChance = 0;
        for (Reward reward : rewards) {
            totalChance += reward.getChance();
        }

        double randomValue = random.nextDouble() * 100;
        double currentSum = 0;

        for (Reward reward : rewards) {
            currentSum += reward.getChance();
            if (randomValue <= currentSum) {
                return reward;
            }
        }

        return rewards.get(0);
    }

    public List<Reward> getRewards() {
        return new ArrayList<>(rewards);
    }
} 