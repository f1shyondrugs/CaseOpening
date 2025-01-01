# CaseOpening Plugin

A Minecraft plugin that adds CS:GO-style crate opening mechanics to your server.

## Dependencies
- Vault
- Essentials
- DecentHolograms

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

## Configuration

### config.yml
```yaml
messages:
	prefix: "&8[&6Crates&8] "
	no-permission: "%prefix%&cYou don't have permission to do this!"

hologram:
	offset: 2.5 # Height offset for holograms
	lines:
		"&e%crate_name%"
		"&eRight Click to open"
		"&eLeft Click to view contents"
        
shop:
	settings:
		title: "&8Key Shop"
		size: 27 # Inventory size (must be multiple of 9)
		fill-empty: true
		filler-material: BLACK_STAINED_GLASS_PANE
```

### cases.yml
```yaml
crates:
	common: # Crate ID
		display-name: "&fCommon Crate"
		block-type: CHEST # Material type for the crate
		hologram:
			name: "&6Common Crate"
			lines: # Custom hologram lines (optional)
				- "&6⭐ Common Crate ⭐"
				- "&eRight Click with key to open"
				- "&eLeft Click to preview"
		key:
			price: 1000 # Key price in shop
			material: TRIPWIRE_HOOK
			name: "&aCommon Key"
			lore:
				- "&7Use this key to open"
				- "&7a Common Crate"
		    shop:
				amount: 1 # Amount per purchase
				slot: 11 # Slot in shop GUI
				additional_lore: # Additional lore in shop
					- "&7A basic key that opens"
					- "&7common crates"
		rewards:
			- chance: 70.0 # Drop chance (total should be 100)
				type: ITEM # ITEM, COMMAND, or MONEY
				material: DIAMOND
				amount: 3
				display-name: "&b3 Diamonds"
				display-item: DIAMOND # Item shown in preview
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
