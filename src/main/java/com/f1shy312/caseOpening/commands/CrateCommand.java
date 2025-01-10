package com.f1shy312.caseOpening.commands;

import com.f1shy312.caseOpening.main;
import com.f1shy312.caseOpening.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import de.tr7zw.nbtapi.NBTItem;

public class CrateCommand implements CommandExecutor, TabCompleter {
    private final main plugin;
    private final File casesFile;

    public CrateCommand(main plugin) {
        this.plugin = plugin;
        this.casesFile = new File(plugin.getDataFolder(), "cases.yml");
    }

    private FileConfiguration getCasesConfig() {
        if (!casesFile.exists()) {
            plugin.getLogger().severe("cases.yml does not exist!");
            return new YamlConfiguration();
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(casesFile);
        if (!config.contains("crates")) {
            plugin.getLogger().severe("cases.yml does not contain a 'crates' section!");
        } else {
            ConfigurationSection cratesSection = config.getConfigurationSection("crates");
            //if (cratesSection != null) {
            //    plugin.getLogger().info("Available crates: " + String.join(", ", cratesSection.getKeys(false)));
            //}
        }
        return config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ColorUtils.formatMessage(plugin, "&cUsage: /crate <give|place|shop|reload|info|help> [args]"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give" -> handleGiveCommand(sender, args);
            case "place" -> handlePlaceCommand(sender, args);
            case "shop" -> handleShopCommand(sender);
            case "reload" -> handleReloadCommand(sender);
            case "info" -> handleInfoCommand(sender);
            case "help" -> handleHelpCommand(sender);
            case "additem" -> handleAddItemCommand(sender, args);
            default -> sender.sendMessage(ColorUtils.formatMessage(plugin, "&cUnknown subcommand. Use /crate help for commands."));
        }

        return true;
    }

    private void handleGiveCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("caseopening.command.give")) {
            sender.sendMessage(ColorUtils.formatMessage(plugin, plugin.getConfig().getString("messages.no-permission")));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(ColorUtils.formatMessage(plugin, "&cUsage: /crate give <player> <type> [amount]"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ColorUtils.formatMessage(plugin, "&cPlayer not found!"));
            return;
        }

        String crateType = args[2].toLowerCase();
        if (plugin.getCrateManager().getCrate(crateType) == null) {
            sender.sendMessage(ColorUtils.formatMessage(plugin, "&cInvalid crate type!"));
            return;
        }

        int amount = 1;
        if (args.length > 3) {
            try {
                amount = Integer.parseInt(args[3]);
                if (amount < 1) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                sender.sendMessage(ColorUtils.formatMessage(plugin, "&cAmount must be a positive number!"));
                return;
            }
        }

        plugin.getKeyManager().givePhysicalKey(target, crateType, amount);
        if (sender != target) {
            sender.sendMessage(ColorUtils.formatMessage(plugin, "&aGave " + amount + " " + crateType + " key(s) to " + target.getName()));
        }
    }

    private void handleShopCommand(CommandSender sender) {
        if (!sender.hasPermission("caseopening.command.shop")) {
            sender.sendMessage(ColorUtils.formatMessage(plugin, plugin.getConfig().getString("messages.no-permission")));
            return;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.formatMessage(plugin, "&cThis command can only be used by players!"));
            return;
        }

        plugin.getShopManager().openShop(player);
    }

    private void handlePlaceCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("caseopening.command.place")) {
            sender.sendMessage(ColorUtils.formatMessage(plugin, plugin.getConfig().getString("messages.no-permission")));
            return;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.formatMessage(plugin, "&cThis command can only be used by players!"));
            return;
        }

        if (args.length != 2) {
            player.sendMessage(ColorUtils.formatMessage(plugin, "&cUsage: /crate place <type>"));
            return;
        }

        String crateType = args[1].toLowerCase();
        if (plugin.getCrateManager().getCrate(crateType) == null) {
            player.sendMessage(ColorUtils.formatMessage(plugin, "&cInvalid crate type!"));
            return;
        }

        player.setMetadata("placing_crate", new FixedMetadataValue(plugin, crateType));
        player.sendMessage(ColorUtils.formatMessage(plugin, "&aRight click a block to place the crate!"));
    }

    private void handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("caseopening.command.reload")) {
            sender.sendMessage(ColorUtils.formatMessage(plugin, plugin.getConfig().getString("messages.no-permission")));
            return;
        }

        try {
            plugin.reloadConfig();
            
            plugin.getCrateManager().reloadCrates();
            plugin.getKeyManager().loadKeys();
            plugin.getShopManager().loadShopItems();
            
            sender.sendMessage(ColorUtils.formatMessage(plugin, plugin.getConfig().getString("messages.configs-reloaded")));
        } catch (Exception e) {
            sender.sendMessage(ColorUtils.formatMessage(plugin, plugin.getConfig().getString("messages.reload-error")));
            plugin.getLogger().severe("Error during reload: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleInfoCommand(CommandSender sender) {
        if (!sender.hasPermission("caseopening.command.info")) {
            sender.sendMessage(ColorUtils.formatMessage(plugin, plugin.getConfig().getString("messages.no-permission")));
            return;
        }

        List<String> infoMessages = new ArrayList<>();
        infoMessages.add("&8&l&m-----------------");
        infoMessages.add("&6&lCase Opening &7v" + plugin.getDescription().getVersion());
        infoMessages.add("&8&l&m-----------------");
        infoMessages.add("&7Made by &eDas_F1sHy312");
        infoMessages.add("&7GitHub: &ehttps://github.com/f1shyondrugs/caseopening");
        infoMessages.add("");
        infoMessages.add("&7Dependencies:");
        infoMessages.add("&8- &eVault");
        infoMessages.add("&8- &eEssentials");
        infoMessages.add("&8- &eDecentHolograms");
        infoMessages.add("");
        infoMessages.add("&7Use &e/crate help &7for commands");
        infoMessages.add("&8&l&m-----------------");

        for (String message : infoMessages) {
            sender.sendMessage(ColorUtils.colorize(message));
        }
    }

    private void handleHelpCommand(CommandSender sender) {
        if (!sender.hasPermission("caseopening.command.help")) {
            sender.sendMessage(ColorUtils.formatMessage(plugin, plugin.getConfig().getString("messages.no-permission")));
            return;
        }

        List<String> helpMessages = new ArrayList<>();
        helpMessages.add("&8&l&m-----------------");
        helpMessages.add("&6&lCase Opening Help");
        helpMessages.add("&8&l&m-----------------");
        
        // Basic commands for all players
        helpMessages.add("&6Basic Commands:");
        helpMessages.add("&e/crate help &7- Show this help menu");
        helpMessages.add("&e/crate info &7- Show plugin information");
        helpMessages.add("&e/crate shop &7- Open the key shop");
        
        // Admin commands
        if (sender.hasPermission("caseopening.admin")) {
            helpMessages.add("");
            helpMessages.add("&6Admin Commands:");
            helpMessages.add("&e/crate give <player> <type> [amount] &7- Give keys to a player");
            helpMessages.add("&e/crate place <type> &7- Place a crate");
            helpMessages.add("&e/crate reload &7- Reload plugin configuration");
            helpMessages.add("&e/crate additem <type> &7- Add held item to crate rewards");
        }
        
        helpMessages.add("&8&l&m-----------------");

        for (String message : helpMessages) {
            sender.sendMessage(ColorUtils.colorize(message));
        }
    }

    private void handleAddItemCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.formatMessage(plugin, "&cThis command can only be used by players!"));
            return;
        }

        if (!sender.hasPermission("caseopening.command.additem")) {
            sender.sendMessage(ColorUtils.formatMessage(plugin, plugin.getConfig().getString("messages.no-permission")));
            return;
        }

        if (args.length != 2) {
            sender.sendMessage(ColorUtils.formatMessage(plugin, "&cUsage: /crate additem <case>"));
            return;
        }

        String crateId = args[1].toLowerCase();
        ItemStack heldItem = player.getInventory().getItemInMainHand();

        if (heldItem.getType() == Material.AIR) {
            sender.sendMessage(ColorUtils.formatMessage(plugin, "&cYou must hold an item in your main hand!"));
            return;
        }

        try {
            File casesFile = new File(plugin.getDataFolder(), "cases.yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(casesFile);
            
            ConfigurationSection crateSection = config.getConfigurationSection("crates." + crateId);
            if (crateSection == null) {
                sender.sendMessage(ColorUtils.formatMessage(plugin, "&cInvalid crate type!"));
                return;
            }

            List<Map<?, ?>> rewards = crateSection.getMapList("rewards");
            
            // Create new reward entry
            Map<String, Object> newReward = new HashMap<>();
            newReward.put("chance", 10.0); // Default chance
            newReward.put("type", "ITEM");
            newReward.put("material", heldItem.getType().toString());
            newReward.put("amount", heldItem.getAmount());
            newReward.put("display-name", heldItem.hasItemMeta() && heldItem.getItemMeta().hasDisplayName() 
                ? heldItem.getItemMeta().getDisplayName() 
                : "&f" + formatMaterialName(heldItem.getType().toString()));
            newReward.put("display-item", heldItem.getType().toString());

            // Add NBT data if present
            try {
                Class.forName("de.tr7zw.nbtapi.NBTItem");
                NBTItem nbtItem = new NBTItem(heldItem);
                if (!nbtItem.toString().equals("{}")) {
                    newReward.put("nbt", nbtItem.toString());
                }
            } catch (ClassNotFoundException e) {
                plugin.getLogger().warning("NBTAPI not found - NBT data will not be saved");
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save NBT data: " + e.getMessage());
            }

            // Add enchantments if present (as backup if NBT fails)
            if (heldItem.hasItemMeta() && heldItem.getItemMeta().hasEnchants()) {
                Map<String, Integer> enchants = new HashMap<>();
                heldItem.getEnchantments().forEach((enchant, level) -> 
                    enchants.put(enchant.getKey().getKey(), level));
                newReward.put("enchantments", enchants);
            }

            // Add item meta if present
            if (heldItem.hasItemMeta() && heldItem.getItemMeta().hasLore()) {
                newReward.put("lore", heldItem.getItemMeta().getLore());
            }

            rewards.add(newReward);
            crateSection.set("rewards", rewards);
            
            config.save(casesFile);
            plugin.getCrateManager().reloadCrates();
            
            sender.sendMessage(ColorUtils.formatMessage(plugin, "&aSuccessfully added item to " + crateId + " crate!"));
            
        } catch (Exception e) {
            sender.sendMessage(ColorUtils.formatMessage(plugin, "&cError adding item to crate: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    private String formatMaterialName(String material) {
        return material.toLowerCase()
                .replace('_', ' ')
                .substring(0, 1).toUpperCase() + 
                material.toLowerCase()
                .replace('_', ' ')
                .substring(1);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("give");
            completions.add("place");
            completions.add("shop");
            completions.add("reload");
            completions.add("info");
            completions.add("help");
            completions.add("additem");
            return filterCompletions(completions, args[0]);
        }

        if (!sender.hasPermission("caseopening.admin")) {
            return completions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("place")) {
            String subCommand = args[0].toLowerCase();
            var cratesSection = getCasesConfig().getConfigurationSection("crates");
            if (cratesSection != null) {
                completions.addAll(cratesSection.getKeys(false));
            }
            return filterCompletions(completions, args[1]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            Bukkit.getOnlinePlayers().forEach(player -> completions.add(player.getName()));
            return filterCompletions(completions, args[1]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            var cratesSection = getCasesConfig().getConfigurationSection("crates");
            if (cratesSection != null) {
                completions.addAll(cratesSection.getKeys(false));
            }
            return filterCompletions(completions, args[2]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("additem")) {
            var cratesSection = getCasesConfig().getConfigurationSection("crates");
            if (cratesSection != null) {
                completions.addAll(cratesSection.getKeys(false));
            }
            return filterCompletions(completions, args[1]);
        }

        return completions;
    }

    private List<String> filterCompletions(List<String> completions, String partial) {
        String lowercasePartial = partial.toLowerCase();
        return completions.stream()
                .filter(str -> str.toLowerCase().startsWith(lowercasePartial))
                .collect(Collectors.toList());
    }
} 