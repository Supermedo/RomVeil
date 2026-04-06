<h1 align="center">
  <img src="app/src/main/res/drawable-nodpi/ic_romveil_logo.png" alt="RomVeil Logo" width="120">
  <br>
  RomVeil
</h1>

<p align="center">
  <b>A typography-centric, premium retro game launcher designed for simplicity and style on Android.</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84.svg?style=flat-square&logo=android" alt="Platform">
  <img src="https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square" alt="License">
</p>

---

## 🌟 About The App

**RomVeil** is not an emulator itself, but a beautiful **front-end launcher** that brings all your retro games together in one cinematic interface. 

Many emulation front-ends suffer from "grid fatigue"—endless, cluttered screens of squares that feel overwhelming to navigate. **RomVeil was built to fix that.** By taking inspiration from modern, premium streaming services, RomVeil focuses heavily on **elegant typography, glassmorphism, and large, beautiful background art**. It makes your game collection feel like a curated gallery rather than a chaotic file system.

### Why RomVeil?
* **Cinematic Experience:** Games are presented with massive, edge-to-edge background art, clean modern fonts, and smooth blurring effects.
* **No-Fuss Setup:** Just tell RomVeil where your games are. It automatically reads the folder name to figure out what console it is (e.g., "SNES", "Switch", "GameCube") and does the rest.
* **Zero-Click Scraping:** You don't have to manually download box art. RomVeil's background engine silently grabs high-quality covers, release years, and game descriptions from the internet automatically.
* **Play With One Button:** Press "Play." That's it. RomVeil automatically knows exactly how to talk to over 50 different Android emulators, bypassing the complex setup normally required to launch games.

---

## ✨ Features

* **Adaptive Premium UI:** The interface automatically scales perfectly. Whether you are on a compact phone held vertically, a wide tablet, or an Android-based handheld gaming console, the layout adjusts to fit the screen flawlessly.
* **Controller Friendly:** Designed to be used with a physical gamepad. You can navigate the entire application—scrolling through games, changing platforms, and opening settings—using only the D-Pad, bumpers, and face buttons.
* **Smart Emulator Bridge:** RomVeil fixes Android 11+ storage limitations (Scoped Storage) automatically. It perfectly launches standalone emulators (like DuckStation, PPSSPP, Dolphin, Redream) and RetroArch cores without you needing to fiddle with file permissions.
* **Modern Console Support:** Not just for 8-bit games. RomVeil natively supports modern platforms including **Nintendo Switch** (Eden, Yuzu, Sudachi, Uzuy) and **Nintendo 3DS** (Citra, Lime3DS).

---

## 🎮 How To Use

1. **Download Emulators:** Install your favorite standalone emulators (like PPSSPP, DuckStation) or RetroArch from the Play Store or GitHub.
2. **Add a Folder:** Open RomVeil, go to Settings -> Library -> Add Folder.
3. **Select Your ROMs:** Pick the folder on your phone containing your game files.
4. **Let It Scrape:** RomVeil will automatically recognize the games and start downloading their cover art.
5. **Play:** Tap a game in the main menu. If you don't have the right emulator installed, RomVeil will instantly offer a button to "GET" it and take you straight to the download page.

---

## 🛠️ For Developers

RomVeil is open source and built using modern Android architecture:
- **Language:** Kotlin
- **UI:** Jetpack Compose natively
- **Architecture:** MVVM, Dagger Hilt, Room Database, Retrofit
- **APIs Used:** ScreenScraper.fr, LibRetro, TheGamesDB, RAWG, MobyGames.

### Building from Source
1. Clone the repository:
   ```bash
   git clone https://github.com/Supermedo/RomVeil.git
   ```
2. Open in Android Studio.
> **Note on Scraper Credentials:** The ScreenScraper developer credentials have been safely extracted. To compile the app, add your own developer credentials to the locally generated `local.properties` file:
> ```properties
> screenscraper.dev.id="your_dev_id"
> screenscraper.dev.password="your_dev_password"
> ```

## ⚖️ Disclaimer
RomVeil is strictly a frontend launcher. It **does not** include any ROMs, BIOS files, copyrighted materials, or emulator binaries. Users are responsible for supplying their own legally obtained game dumps.