# Obsidian Tears

Obsidian Tears is a NeoForge Minecraft mod that adds player-built teleport monuments and a handheld teleport item.

Players can build a teleport monument by placing a Redstone Block on top of Crying Obsidian. The mod records the creator, dimension, coordinates, and sequence number, then asks the creator to name it. A floating label is shown above the monument, and players can use the Obsidian Tear item to open a teleport list.

## Features

- Create teleport monuments with vanilla blocks:
  - Bottom: Crying Obsidian
  - Top: Redstone Block
- Name each monument when it is created.
- Show the monument name and sequence number as an in-world label.
- Remove saved monument data when either monument block is broken.
- Add the Obsidian Tear item.
- Right-click with Obsidian Tear to open a teleport menu.
- Teleport across dimensions, including the Nether and the End.
- Preserve common riding state when teleporting on mounts.
- Attempt to preserve entities leashed to the player during teleport.

## Compatibility

- Minecraft: `26.1.2`
- NeoForge target: `26.1.2.0-beta`
- Runtime tested around NeoForge `26.1.2.30-beta`
- Side: client and server

Because this targets a beta NeoForge/Minecraft line, small API changes between beta builds may still require compatibility fixes.

## Usage

1. Place Crying Obsidian.
2. Place a Redstone Block directly on top of it.
3. Enter a name in the naming screen.
4. Craft or obtain an Obsidian Tear.
5. Right-click with Obsidian Tear to open the teleport menu.
6. Click a monument entry to teleport there.

Breaking the Redstone Block or the Crying Obsidian removes the monument record.

## Crafting

Obsidian Tear is shapelessly crafted from:

- 1 Gold Nugget
- 1 Redstone Dust
- 1 Glowstone Dust

The item uses the Gold Nugget model with an enchanted glint.

## Building

Requirements:

- JDK 25
- Gradle wrapper included in this repository

Build:

```powershell
.\gradlew.bat build
```

The built mod jar will be generated in:

```text
build/libs/
```

## Project Structure

- `src/main/java/com/obsidiantears/neoforge/` - mod source code
- `src/main/resources/assets/obsidiantears/` - client assets and translations
- `src/main/resources/data/obsidiantears/` - recipes and data files
- `src/main/templates/META-INF/neoforge.mods.toml` - generated mod metadata template

## License

This project is released under the MIT License. See [LICENSE](LICENSE).

This repository was created from the NeoForge MDK template. The original template license is kept in [TEMPLATE_LICENSE.txt](TEMPLATE_LICENSE.txt).
