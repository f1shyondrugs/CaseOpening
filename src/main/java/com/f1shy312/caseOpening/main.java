package com.f1shy312.caseOpening;

import com.f1shy312.caseOpening.commands.CrateCommand;
import com.f1shy312.caseOpening.listeners.CrateInteractListener;
import com.f1shy312.caseOpening.listeners.GUIListener;
import com.f1shy312.caseOpening.listeners.CrateOpeningListener;
import com.f1shy312.caseOpening.listeners.CrateContentsListener;
import com.f1shy312.caseOpening.listeners.ChatListener;
import com.f1shy312.caseOpening.managers.CrateManager;
import com.f1shy312.caseOpening.managers.KeyManager;
import com.f1shy312.caseOpening.managers.HologramManager;
import com.f1shy312.caseOpening.managers.ShopManager;
import com.f1shy312.caseOpening.managers.LogManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import java.io.File;

public final class main extends JavaPlugin {
    private static main instance;
    private CrateManager crateManager;
    private KeyManager keyManager;
    private HologramManager hologramManager;
    private ShopManager shopManager;
    private LogManager logManager;

    @Override
    public void onEnable() {
        getLogger().info("CaseOpening plugin is starting...");
        instance = this;
        
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().severe("Vault not found! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        if (getServer().getPluginManager().getPlugin("Essentials") == null) {
            getLogger().severe("Essentials not found! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        if (getServer().getPluginManager().getPlugin("DecentHolograms") == null) {
            getLogger().severe("DecentHolograms not found! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        if (!new File(getDataFolder(), "cases.yml").exists()) {
            saveResource("cases.yml", false);
        }
        
        FileConfiguration casesConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "cases.yml"));
        saveResource("keys.yml", false);

        this.crateManager = new CrateManager(this, new File(getDataFolder(), "cases.yml"));
        this.keyManager = new KeyManager(this);
        this.hologramManager = new HologramManager(this);
        this.shopManager = new ShopManager(this);
        this.logManager = new LogManager(this);
        
        getCommand("crate").setExecutor(new CrateCommand(this));
        
        getServer().getPluginManager().registerEvents(new CrateInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new CrateOpeningListener(this), this);
        getServer().getPluginManager().registerEvents(new CrateContentsListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        
        getLogger().info("CaseOpening plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("CaseOpening plugin is shutting down...");
        if (hologramManager != null) {
            hologramManager.removeAllHolograms();
        }
        if (crateManager != null) {
            crateManager.saveCrateLocations();
        }
        if (keyManager != null) {
            keyManager.saveKeys();
        }
        getLogger().info("CaseOpening plugin has been disabled!");
    }

    public static main getInstance() {
        return instance;
    }

    public CrateManager getCrateManager() {
        return crateManager;
    }

    public KeyManager getKeyManager() {
        return keyManager;
    }

    public HologramManager getHologramManager() {
        return hologramManager;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public LogManager getLogManager() {
        return logManager;
    }
}
