# CaseOpening Plugin

A Minecraft plugin that adds CS:GO/CS2-style crate opening mechanics to your server.

## Dependencies
- Vault
- Essentials
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
    hologram:
        offset: 2.5 # Height offset for holograms
        lines:
            - "&e%crate_name%"
            - "&eRight Click to open"
            - "&eLeft Click to view contents"
    shop:
        settings:
            title: "&8Key Shop"
            size: 27 # Inventory size (must be multiple of 9)
            fill-empty: true
            filler-material: BLACK_STAINED_GLASS_PANE
```
###cases.yml
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
              material: DIAMOND_SWORD
              commands:
                - "give %player% diamond_sword 1"
              display-name: "&bDiamond Sword"
              display-item: DIAMOND_SWORD
            - chance: 10.0
              type: MONEY
              material: GOLD_INGOT
              amount: 1000
              display-name: "&61000 Coins"
              display-item: GOLD_INGOT
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

## Notes
- All color codes use `&` for formatting
- Supports hex colors with `#RRGGBB` format
- Total reward chances MUST add up to 100 (if not, unexpected behavior may occur)
- Shop slots start from 0 and must be less than shop size
- Block types must be valid Minecraft materials
- Hologram lines can be customized per crate or use default configuration
- Custom messages can include the prefix placeholder

