# Minecraft Color Highlighter for IntelliJ Platform

`MinecraftColorIDEA` is a JetBrains IDE port of [`vscode-mc-color`](https://github.com/nobu-sh/vscode-mc-color), focused on Minecraft formatting codes, inline translation previews, and editor-side color tooling.

## Features

- Highlights Minecraft section-sign formatting codes (`\u00a7`), escaped `\u00a7` sequences, `#RRGGBB`, `#AARRGGBB`, `0xRRGGBB`, and `0xAARRGGBB`.
- Renders inline previews for localized text resolved from common Minecraft APIs such as `StatCollector`, `I18n`.
- Preserves formatting state in previews, including color, bold, italic, underline, strikethrough, reset, and obfuscated markers.
- Updates localization previews immediately when project language files change, with file-level caching for faster refreshes.
- Adds inline source markers:
  - click hex markers to open a color picker with alpha support
  - click Minecraft section-sign markers to choose another color or formatting code from a popup list
- Supports project-level locale overrides with fallback order:
  1. project preferred locale
  2. project secondary locale
  3. `en_us`
- Supports project-aware version detection with this priority:
  1. project override
  2. detected project version
  3. global default
  4. built-in default
- Java edition version-aware behavior covers Minecraft `1.7.10` through `1.21`.

## Requirements

- IntelliJ Platform `2025.1+`
- Gradle wrapper included in this repository

## Build

On Windows:

```powershell
.\gradlew.bat buildPlugin
```

On macOS or Linux:

```bash
./gradlew buildPlugin
```

The packaged plugin archive will be created in `build/distributions/`.

## Run Tests

On Windows:

```powershell
.\gradlew.bat test
```

On macOS or Linux:

```bash
./gradlew test
```

## Install

1. Build the plugin.
2. In IntelliJ IDEA or another JetBrains IDE, open `Settings/Preferences -> Plugins`.
3. Click the gear icon.
4. Choose `Install Plugin from Disk...`.
5. Select the ZIP file from `build/distributions/`.
6. Restart the IDE.

## Configuration

Global settings are available in:

`Settings/Preferences -> Tools -> Minecraft Color Highlighter`

Project-specific overrides are available in:

`Settings/Preferences -> Tools -> Minecraft Color Highlighter`

You can configure:

- enable or disable highlighting
- marker style
- prefixes
- preferred and secondary locales
- project version override
- extra localization method names

## Version Detection

The plugin checks version sources in this order:

1. project override from plugin settings
2. `mcmod.info`, `mods.toml`, `fabric.mod.json`, or `quilt.mod.json`
3. main-class style version hints in project source files
4. Gradle files such as `build.gradle`, `build.gradle.kts`, and `gradle.properties`

Detected values are cached and reused until relevant project files change.

## Notes

- Language resources from the current project are preferred over local dependency resources.
- Inline previews intentionally avoid recoloring the localization key literal in source code.
- Source marker editing is designed to stay responsive during normal typing and language-file edits.

## License

This project is licensed under the GNU Lesser General Public License v3.0.
