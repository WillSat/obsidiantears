# Obsidian Tears

Obsidian Tears is a NeoForge Minecraft mod that adds player-built teleport monuments and a handheld teleport item.

Place a Redstone Block on top of Crying Obsidian to create a monument. Name it, then use an Obsidian Tear to teleport across dimensions.

## Features

- Build teleport monuments from vanilla blocks (Crying Obsidian + Redstone Block)
- Name each monument on creation
- In-world floating label with colored sequence number
- Permanent purple portal particles ambient around each monument
- Purple teleport particle burst on arrival
- Tab-based dimension switching in the teleport menu (All / Overworld / Nether / End)
- Dimension grouping with styled section headers
- Colored sequence numbers (6-color cycle: red, purple, indigo, cyan, lime, yellow)
- Cross-dimension teleport with mount and leash preservation
- Multi-language support: English, 简体中文, 日本語, 한국어

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
6. Switch tabs to filter by dimension
7. Click a monument entry to teleport

Breaking either monument block removes the record and its label.

## Crafting

Obsidian Tear (shapeless):

- 1 Gold Nugget
- 1 Redstone Dust
- 1 Glowstone Dust

The item uses the Gold Nugget model with an enchanted glint.

## Building

Requirements:

- JDK 25
- Gradle wrapper included

```powershell
.\gradlew.bat build
```

Output: `build/libs/obsidiantears-1.1.0.jar`

## Project Structure

- `src/main/java/com/obsidiantears/neoforge/` — mod source
- `src/main/resources/assets/obsidiantears/lang/` — translations (en_us, zh_cn, ja_jp, ko_kr)
- `src/main/resources/assets/obsidiantears/` — client assets
- `src/main/resources/data/obsidiantears/` — recipes and data

## License

MIT. See [LICENSE](LICENSE).
