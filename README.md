# GLYPH

A high-fidelity, typography-centric Android frontend for retro gaming.

Glyph is designed exclusively for horizontal orientation and controller navigation, prioritizing aesthetic minimalism. It does not emulate games — it discovers your ROM collection, scrapes metadata and artwork, then launches games in your preferred emulator apps via Android Intents.

## Architecture

```
com.glyph.launcher/
├── data/
│   ├── local/          # Room database, entities, DAOs
│   ├── remote/         # ScreenScraper.fr API (Retrofit)
│   ├── repository/     # Single source of truth
│   └── scanner/        # SAF-based ROM file discovery
├── di/                 # Hilt dependency injection modules
├── domain/model/       # Platform definitions, emulator info
├── ui/
│   ├── theme/          # Colors, typography (Inter font), Material3 theme
│   ├── main/           # Main screen: blurred background + center-snap list
│   ├── setup/          # First-run setup flow (folder picker, scan, scrape)
│   └── dialog/         # Emulator picker dialog
└── util/               # Hash computation, controller input, emulator launching, preferences
```

**Tech Stack:** Kotlin · Jetpack Compose · Room · Hilt · Retrofit · Coil · DataStore

## Requirements

- **Android Studio** Iguana (2024.1) or later
- **JDK 17**
- **Min SDK 26** (Android 8.0 Oreo)
- **Target SDK 34** (Android 14)

## Setup

1. **Clone** the repository and open in Android Studio.
2. **Sync Gradle** — all dependencies are declared in `gradle/libs.versions.toml`.
3. **ScreenScraper API credentials** (optional, for metadata scraping):
   - Register at [screenscraper.fr](https://www.screenscraper.fr/)
   - Add your dev credentials in `app/build.gradle.kts`:
     ```kotlin
     buildConfigField("String", "SCREENSCRAPER_DEV_ID", "\"your_dev_id\"")
     buildConfigField("String", "SCREENSCRAPER_DEV_PASSWORD", "\"your_dev_password\"")
     ```
4. **Build and run** on a physical device or emulator.

## How It Works

### First Launch (Setup Flow)
1. User selects a root ROM folder via Android's Storage Access Framework.
2. The app recursively scans for ROM files, mapping extensions to platforms:
   - `.sfc`, `.smc` → SNES
   - `.nes` → NES
   - `.gba` → GBA
   - `.gb` → Game Boy
   - `.gbc` → Game Boy Color
   - `.n64`, `.z64`, `.v64` → N64
   - `.nds` → Nintendo DS
   - `.gen`, `.md`, `.smd` → Genesis
   - `.iso`, `.chd`, `.cue`, `.pbp` → PlayStation
   - `.iso`, `.cso` → PSP
   - `.zip` → Arcade
3. Optionally scrapes metadata from ScreenScraper.fr (hash-based lookup, then filename fallback).
4. Downloads fanart/screenshot backgrounds to local storage.

### Main Interface
- **Layer 1 (Background):** Full-screen, heavily blurred (32dp) game artwork with 40% black overlay. Cross-fades when scrolling (~300ms).
- **Layer 2 (Typography):** Center-snapping vertical list with three visual tiers:
  - **Selected** (center): 42sp bold, full opacity
  - **Near** (±1): 28sp semi-bold, 60% opacity
  - **Far** (±2+): 20sp regular, 25% opacity

### Controls
| Input | Action |
|-------|--------|
| D-Pad Up/Down | Navigate list |
| A / Enter | Launch game |
| B / Back | Back |
| L1 / R1 | Cycle platform filter |
| Start | Open settings/setup |
| Touch swipe | Scroll list |
| Tap center | Launch game |

### Emulator Launching
When pressing A on a game, Glyph sends an `ACTION_VIEW` intent to the selected emulator with the ROM's content URI. If multiple emulators are installed for a platform, a picker dialog appears with an "always use this" option.

**Supported emulators include:**
- RetroArch (universal)
- Snes9x EX+, NES.emu, GBA.emu, GBC.emu, MD.emu (Robert Broglia's .emu series)
- DuckStation (PlayStation)
- PPSSPP (PSP)
- DraStic (Nintendo DS)
- Mupen64Plus FZ (N64)

## License

This project is provided as-is for personal use.
