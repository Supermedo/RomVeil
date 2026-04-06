<h1 align="center">
  <img src="app/src/main/res/drawable-nodpi/ic_romveil_logo.png" alt="RomVeil Logo" width="120">
  <br>
  RomVeil
</h1>

<p align="center">
  <b>A typography-centric retro game launcher designed for simplicity and style on Android.</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84.svg?style=flat-square&logo=android" alt="Platform">
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF.svg?style=flat-square&logo=kotlin" alt="Language">
  <img src="https://img.shields.io/badge/UI-Jetpack_Compose-4285F4.svg?style=flat-square&logo=jetpackcompose" alt="Jetpack Compose">
  <img src="https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square" alt="License">
</p>

---

## 🌟 Overview

**RomVeil** is a premium, beautifully crafted front-end launcher for retro game emulation on Android. Rather than bombarding the user with complex toggles or clunky grid menus, RomVeil focuses on **typography, glassmorphism, and seamless immersion**. 

It handles the heavy lifting of metadata scraping, cover art downloading, and emulator mapping so you can focus on what matters: **playing your games.**

### ✨ Key Features

- **Premium UI/UX:** Built entirely with Jetpack Compose featuring adaptive layouts for both phones and tablets, sleek dark mode aesthetics, and glassmorphic overlays.
- **Auto-Scraping Engine:** Seamlessly pulls metadata, release years, and high-quality box art using LibRetro, ScreenScraper.fr, TheGamesDB, RAWG, and MobyGames.
- **Intelligent Platform Detection:** Uses Android's Storage Access Framework (SAF) to scan directories and automatically assign ROM extensions to the correct console.
- **Controller Navigation:** Full gamepad support (D-Pad, Bumpers, Face buttons) for a console-like experience on Android handhelds.
- **Extensive Emulator Support:** Works out-of-the-box with over 50 standalone emulators and RetroArch cores.
    - Includes mapping for modern systems like **Nintendo Switch** (Eden, Yuzu, Uzuy, Suyu, Skyline) and **Nintendo 3DS** (Citra, Lime3DS, Panda3DS).

## 🛠️ Built With

- **[Kotlin](https://kotlinlang.org/)** - Primary language
- **[Jetpack Compose](https://developer.android.com/jetpack/compose)** - Declarative UI toolkit
- **[Room](https://developer.android.com/training/data-storage/room)** - Local SQLite database for library caching
- **[Dagger Hilt](https://dagger.dev/hilt/)** - Dependency injection
- **[Retrofit](https://square.github.io/retrofit/) & [OkHttp](https://square.github.io/okhttp/)** - Network requests for metadata APIs
- **[Coil](https://coil-kt.github.io/coil/)** - Image loading and caching

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug (or newer)
- Android SDK 35
- Java 17

### Building from Source
1. Clone the repository:
   ```bash
   git clone https://github.com/Supermedo/RomVeil.git
   ```
2. Open the project in Android Studio.
3. Sync the Gradle files.
4. Build and Run! 

> **Note on Scraper Credentials:** The ScreenScraper developer credentials have been securely extracted to `local.properties`. If you intend to compile the app and utilize the fast ScreenScraper engine, you will need to add your own developer credentials to the `local.properties` file:
> ```properties
> screenscraper.dev.id="your_dev_id"
> screenscraper.dev.password="your_dev_password"
> ```
> Users can still input their own *User* credentials directly within the app settings to bypass quotas!

## 🎮 Supported Systems
RomVeil natively routes ROMs to the correct emulator intents for:
- Nintendo (NES, SNES, N64, GameCube, Wii, Switch)
- Game Boy (GB, GBC, GBA, NDS, 3DS)
- PlayStation (PS1, PS2, PSP)
- Sega (Genesis, 32X, Saturn, Dreamcast)
- Arcade & Neo Geo

## ⚖️ Disclaimer
RomVeil is strictly a frontend launcher. It **does not** include any ROMs, BIOS files, copyrighted materials, or emulator binaries. Users are responsible for supplying their own legally obtained game dumps and downloading emulators from the Google Play Store or corresponding open-source repositories.

## 📄 License
This project is licensed under the MIT License - see the LICENSE file for details.