# Obsidian Tears

Obsidian Tears is a NeoForge Minecraft mod that adds player-built teleport monuments and a handheld teleport item.

Place a Redstone Block on top of Crying Obsidian to create a monument. Name it, then use an Obsidian Tear to teleport across dimensions.

## Features

- Build teleport monuments from vanilla blocks (Crying Obsidian + Redstone Block)
- Name each monument on creation
- In-world floating label with white name and dimension-colored sequence (OVW-1, NTH-1, END-1)
- Per-dimension sequence numbering with auto-renumber on removal
- Dynamic dimension tabs — only dimensions with waypoints appear; mod dimensions supported
- Move waypoints up/down to reorder within a dimension
- Permanent portal particles ambient around each monument
- Teleport feedback overlay (biome, monument label, time & weather in Overworld)
- Biome entry overlay: biome name + time/weather shown on entering a new biome via vanilla movement
- Max 1024 waypoints per dimension limit
- Tab-based dimension switching in the teleport menu
- Cross-dimension teleport with mount and leash preservation
- Coordinate display with one-click clipboard copy
- Cyan-colored Obsidian Tear item name
- Enchanted glint effect on the Obsidian Tear item
- Advancement: "Obsidian Tear Drop" triggered when first obtaining the item
- Multi-language support: English, `简体中文`, `日本語`, `한국어`

## Compatibility

- Minecraft: `26.1.2`
- NeoForge: `26.1.2.0-beta`
- Side: client and server

## Usage

1. Place Crying Obsidian
2. Place a Redstone Block directly on top
3. Enter a name in the naming screen
4. Craft or obtain an Obsidian Tear
5. Right-click with Obsidian Tear to open the teleport menu
6. Switch tabs to filter by dimension (tabs appear dynamically)
7. Click a monument entry to teleport
8. Use ▲/▼ buttons in a dimension tab to reorder waypoints

Breaking either monument block removes the record, renumbers the remaining waypoints in that dimension, and refreshes all labels.

## Crafting

Obsidian Tear (shaped):

```
A A A
A D A
A A A
```

- A: Amethyst Shard (×8)
- D: Diamond (×1)

The item is fire-resistant, unbreakable, and can mine obsidian in 2 seconds.

## Building

Requirements:

- JDK 25
- Gradle wrapper included

```powershell
.\gradlew.bat build
```

Output: `build/libs/obsidiantears-1.4.jar`

## Project Structure

- `src/main/java/com/obsidiantears/neoforge/` — mod source
- `src/main/resources/assets/obsidiantears/lang/` — translations (en_us, zh_cn, ja_jp, ko_kr)
- `src/main/resources/assets/obsidiantears/` — client assets
- `src/main/resources/data/obsidiantears/` — recipes and data

## License

MIT. See [LICENSE](LICENSE).

## Changelog

### 1.4

- Return teleport: right-click menu now includes a button to return to the position before your most recent waypoint teleport (dimension-aware)
- Right-click menu can be closed with the E key
- HUD overlay: third line (time/weather) font enlarged and color changed to white; reduced line spacing
- Recipe changed: 8 Amethyst Shards around 1 Diamond (shaped crafting)
- Obsidian Tear Drop is now fire/lava-resistant, can mine obsidian (2s), unbreakable, and has an enchantment glint
- Advancement "Obsidian Tear Drop" triggers on first obtaining the item

### 1.3

- Biome entry overlay: top-right HUD shown when entering a new biome via vanilla movement (biome name + time/weather in Overworld only)
- Rapid overlay suppression: new display requests ignored while current overlay is still active
- Max waypoint limit: 1024 per dimension with in-game warning on attempt to exceed
- Performance: biome change detection reduced to every 2 seconds per player

### 1.2.2

- Fixed duplicate labels stacking on monuments after reordering waypoints from unloaded chunks
- Fixed `WaypointNamingPacket` updating wrong monument labels when monuments are adjacent
- Fixed label removal AABB overlap that could remove neighboring monument labels
- Tick-based label self-healing: verifies label count, name, and entity properties every 1.5s for loaded monuments
- Fixed "All" tab dimension grouping order in teleport menu
