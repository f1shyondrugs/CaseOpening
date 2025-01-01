package com.f1shy312.caseOpening.models;

import org.bukkit.Material;
import java.util.List;

public class ShopItem {
    private final String id;
    private final String displayName;
    private final Material material;
    private final double price;
    private final int amount;
    private final int slot;
    private final List<String> lore;

    public ShopItem(String id, String displayName, Material material, double price, int amount, int slot, List<String> lore) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.price = price;
        this.amount = amount;
        this.slot = slot;
        this.lore = lore;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Material getMaterial() { return material; }
    public double getPrice() { return price; }
    public int getAmount() { return amount; }
    public int getSlot() { return slot; }
    public List<String> getLore() { return lore; }
} 