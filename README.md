# CaseOpening Plugin

A Minecraft plugin that adds CS:GO/CS2-style crate opening mechanics to your server.

## Dependencies
- Vault
- DecentHolograms
- NBTAPI

## Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/crate help` | Shows help menu | `caseopening.command.help` |
| `/crate info` | Shows plugin information | `caseopening.command.info` |
| `/crate shop` | Opens the key shop | `caseopening.command.shop` |
| `/crate give <player> <type> [amount]` | Gives crate keys to a player | `caseopening.command.give` |
| `/crate place <type>` | Places a crate | `caseopening.command.place` |
| `/crate reload` | Reloads plugin configuration | `caseopening.command.reload` |
| `/crate additem <type>` | Adds held item to crate rewards | `caseopening.command.additem` |

## Permissions
| Permission | Description | Default |
|------------|-------------|---------|
| `caseopening.use` | Allows using basic crate commands | true |
| `caseopening.admin` | Allows using admin crate commands | op |
| `caseopening.crate.destroy` | Allows destroying crates | op |
| `caseopening.crate.open` | Allows opening crates | true |
| `caseopening.crate.preview` | Allows previewing crate contents | true |
| `caseopening.command.help` | Allows using the help command | true |
| `caseopening.command.info` | Allows using the info command | true |
| `caseopening.command.shop` | Allows using the shop command | true |
| `caseopening.command.give` | Allows giving crate keys to players | op |
| `caseopening.command.place` | Allows placing crates | op |
| `caseopening.command.reload` | Allows reloading the plugin | op |
| `caseopening.command.additem` | Allows adding items to crates | op |

## Configuration

### config.yml
```yaml
messages:
  prefix: "&8[&6Crates&8] "
  no-permission: "%prefix%&cYou don't have permission to do this!"
  key-received: "%prefix%&aYou received &e%amount% %key_name% &akeys!"
  crate-placed: "%prefix%&aCrate has been placed successfully!"
  crate-removed: "%prefix%&cCrate has been removed!"
  crate-destroyed: "%prefix%&cCrate has been destroyed!"
  not-enough-keys: "%prefix%&cYou don't have enough keys to open this crate!"
  no-key: "%prefix%&cYou need a key to open this crate!"
  wrong-key: "%prefix%&cThis is not the correct key for this crate!"
  configs-reloaded: "%prefix%&aAll configurations have been reloaded!"
  reload-error: "%prefix%&cError reloading configurations! Check console for details."
  insufficient-funds: "%prefix%&cYou don't have enough money!"
  purchase-success: "%prefix%&aSuccessfully purchased %amount% %key_name% key(s)!"
  already-opening: "&cYou are already opening a case!"
  reward-won: "%prefix%&aYou won: &e%reward_name%&a!"

# GUI Settings
gui:
  # Opening Animation GUI
  opening:
    title: "&8Opening %crate_name%"
    filler-material: BLACK_STAINED_GLASS_PANE
    filler-name: " "
  
  # Contents Preview GUI
  contents:
    title: "&8%crate_name% Contents"
    filler-material: BLACK_STAINED_GLASS_PANE
    filler-name: " "
    reward-display:
      lore:
        - ""
        - "&7Chance: &e%chance%%"
        - "&7Amount: &e%amount%"
        - "&7Type: %reward_type%"
      type-formats:
        money: "&a&lMoney Reward"
        command: "&d&lSpecial Reward"
        item: "&b&lItem Reward"
    more-rewards:
      name: "&e&lMore Rewards..."
      lore:
        - "&7There are &e%remaining_rewards% &7more rewards"
        - "&7that are not shown here."

# Hologram Settings
hologram:
  offset: 2.5
  lines:
    - "&e%crate_name%"
    - "&eRight Click to open"
    - "&eLeft Click to view contents"

# Shop Configuration
shop:
  settings:
    title: "&8Key Shop"
    fill-empty: true
    filler-material: BLACK_STAINED_GLASS_PANE

# Key Settings
keys:
  shop_lore:
    - "&7Price: &a$%price%"
    - "&7Amount: &e%amount%"
    - ""
    - "&eClick to purchase!"
```

### cases.yml
Example crate configuration:
```yaml
crates:
  common:
    display-name: "&fCommon Crate"
    block-type: CHEST
    hologram:
      name: "&6Common Crate"
      lines:
        - "&6⭐ Common Crate ⭐"
        - "&eRight Click with key to open"
        - "&eLeft Click to preview"
    key:
      price: 1000
      material: TRIPWIRE_HOOK
      name: "&aCommon Key"
      lore:
        - "&7Use this key to open"
        - "&7a Common Crate"
      shop:
        amount: 1
        slot: 11
        additional_lore:
          - "&7A basic key that opens"
          - "&7common crates"
    rewards:
      - chance: 70.0
        type: ITEM
        material: DIAMOND
        amount: 3
        display-name: "&b3 Diamonds"
        display-item: DIAMOND
      - chance: 20.0
        type: COMMAND
        commands:
          - "give %player% diamond_sword 1"
        display-name: "&bDiamond Sword"
        display-item: DIAMOND_SWORD
      - chance: 10.0
        type: MONEY
        amount: 1000
        display-name: "&61000 Coins"
        display-item: GOLD_INGOT
        nbt: '{PublicBukkitValues:{"something":something,"something2":something2,"something3":something3}}'
```

## Reward Types
- **ITEM**: Gives specified item(s)
- **COMMAND**: Executes command(s) (%player% placeholder available)
- **MONEY**: Gives money (requires Vault)

## Placeholders
- `%prefix%` - Plugin prefix (in messages)
- `%player%` - Player name (in commands)
- `%amount%` - Amount of keys (in messages)
- `%key_name%` - Key name (in messages)
- `%crate_name%` - Crate name (in holograms)
- `%reward_name%` - Reward name (in messages)
- `%chance%` - Reward chance (in GUI)
- `%reward_type%` - Reward type (in GUI)
- `%remaining_rewards%` - Number of remaining rewards (in GUI)
- `%price%` - Key price (in shop)

## Notes
- All color codes use `&` for formatting
- Supports hex colors with `#RRGGBB` format
- Total reward chances MUST add up to 100 (if not, unexpected behavior may occur)
- Shop slots start from 0 and must be less than shop size
- Block types must be valid Minecraft materials
- Hologram lines can be customized per crate or use default configuration
- Custom messages can include the prefix placeholder

## Support

For issues or feature requests, please use the GitHub issue tracker, contact me on Discord: `f1shyondrugs312` or per email: `info@f1shy312.com`
