package com.glyph.launcher.domain.model

enum class Platform(
    val tag: String,
    val displayName: String,
    val extensions: Set<String>,
    val screenScraperId: Int,
    val defaultEmulators: List<EmulatorInfo>
) {
    NES(
        tag = "nes",
        displayName = "Nintendo Entertainment System",
        extensions = setOf(".nes", ".zip", ".7z"),
        screenScraperId = 3,
        defaultEmulators = listOf(
            EmulatorInfo("com.explusalpha.NesEmu", "NES.emu", null),
            EmulatorInfo("com.nostalgiaemulators.nesfull", "Nostalgia.NES Pro", null),
            EmulatorInfo("com.nostalgiaemulators.neslite", "Nostalgia.NES", null),
            EmulatorInfo("com.retroarch", "RetroArch", null),
            EmulatorInfo("com.retroarch.aarch64", "RetroArch (64-bit)", null),
        )
    ),

    SNES(
        tag = "snes",
        displayName = "Super Nintendo",
        extensions = setOf(".sfc", ".smc", ".zip", ".7z"),
        screenScraperId = 4,
        defaultEmulators = listOf(
            EmulatorInfo("com.explusalpha.Snes9xPlus", "Snes9x EX+", null),
            EmulatorInfo("com.neutronemulation.super_retro_16", "SuperRetro16", null),
            EmulatorInfo("com.retroarch", "RetroArch", null),
            EmulatorInfo("com.retroarch.aarch64", "RetroArch (64-bit)", null),
        )
    ),

    N64(
        tag = "n64",
        displayName = "Nintendo 64",
        extensions = setOf(".n64", ".z64", ".v64", ".zip", ".7z"),
        screenScraperId = 14,
        defaultEmulators = listOf(
            EmulatorInfo("org.mupen64plusae.v3.fzurita", "Mupen64Plus FZ (M64+)", null),
            EmulatorInfo("com.retroarch", "RetroArch", null),
            EmulatorInfo("com.retroarch.aarch64", "RetroArch (64-bit)", null),
        )
    ),

    GB(
        tag = "gb",
        displayName = "Game Boy",
        extensions = setOf(".gb", ".zip", ".7z"),
        screenScraperId = 9,
        defaultEmulators = listOf(
            EmulatorInfo("com.explusalpha.GbcEmu", "GBC.emu", null),
            EmulatorInfo("com.fastemulator.gbc", "My OldBoy!", null),
            EmulatorInfo("it.dbtecno.pizzaboypro", "Pizza Boy GBC Pro", null),
            EmulatorInfo("it.dbtecno.pizzaboy", "Pizza Boy GBC", null),
            EmulatorInfo("com.retroarch", "RetroArch", null),
            EmulatorInfo("com.retroarch.aarch64", "RetroArch (64-bit)", null),
        )
    ),

    GBC(
        tag = "gbc",
        displayName = "Game Boy Color",
        extensions = setOf(".gbc", ".zip", ".7z"),
        screenScraperId = 10,
        defaultEmulators = listOf(
            EmulatorInfo("com.explusalpha.GbcEmu", "GBC.emu", null),
            EmulatorInfo("com.fastemulator.gbc", "My OldBoy!", null),
            EmulatorInfo("it.dbtecno.pizzaboypro", "Pizza Boy GBC Pro", null),
            EmulatorInfo("it.dbtecno.pizzaboy", "Pizza Boy GBC", null),
            EmulatorInfo("com.retroarch", "RetroArch", null),
            EmulatorInfo("com.retroarch.aarch64", "RetroArch (64-bit)", null),
        )
    ),

    GBA(
        tag = "gba",
        displayName = "Game Boy Advance",
        extensions = setOf(".gba", ".zip", ".7z"),
        screenScraperId = 12,
        defaultEmulators = listOf(
            EmulatorInfo("com.explusalpha.GbaEmu", "GBA.emu", null),
            EmulatorInfo("com.fastemulator.gba", "My Boy!", null),
            EmulatorInfo("com.fastemulator.gbafree", "My Boy! Lite", null),
            EmulatorInfo("it.dbtecno.pizzaboygbapro", "Pizza Boy GBA Pro", null),
            EmulatorInfo("com.retroarch", "RetroArch", null),
            EmulatorInfo("com.retroarch.aarch64", "RetroArch (64-bit)", null),
        )
    ),

    NDS(
        tag = "nds",
        displayName = "Nintendo DS",
        extensions = setOf(".nds", ".zip", ".7z"),
        screenScraperId = 15,
        defaultEmulators = listOf(
            EmulatorInfo("com.dsemu.drastic", "DraStic", null),
            EmulatorInfo("me.magnum.melonds", "melonDS", null),
            EmulatorInfo("com.retroarch", "RetroArch", null),
            EmulatorInfo("com.retroarch.aarch64", "RetroArch (64-bit)", null),
        )
    ),

    GENESIS(
        tag = "genesis",
        displayName = "Sega Genesis / Mega Drive",
        extensions = setOf(".md", ".gen", ".smd", ".bin", ".zip", ".7z"),
        screenScraperId = 1,
        defaultEmulators = listOf(
            EmulatorInfo("com.explusalpha.MdEmu", "MD.emu", null),
            EmulatorInfo("com.retroarch", "RetroArch", null),
            EmulatorInfo("com.retroarch.aarch64", "RetroArch (64-bit)", null),
        )
    ),

    SEGA32X(
        tag = "32x",
        displayName = "Sega 32X",
        extensions = setOf(".32x", ".zip", ".7z"),
        screenScraperId = 19,
        defaultEmulators = listOf(
            EmulatorInfo("com.explusalpha.MdEmu", "MD.emu", null),
            EmulatorInfo("com.retroarch", "RetroArch", null),
            EmulatorInfo("com.retroarch.aarch64", "RetroArch (64-bit)", null),
        )
    ),

    SATURN(
        tag = "saturn",
        displayName = "Sega Saturn",
        extensions = setOf(".cue", ".chd", ".iso", ".zip", ".7z"),
        screenScraperId = 22,
        defaultEmulators = listOf(
            EmulatorInfo("org.devmiyax.yabasern.pro", "Yaba Sanshiro 2 Pro", null),
            EmulatorInfo("org.devmiyax.yabasern", "Yaba Sanshiro 2", null),
            EmulatorInfo("com.retroarch", "RetroArch", null),
            EmulatorInfo("com.retroarch.aarch64", "RetroArch (64-bit)", null),
        )
    ),

    DREAMCAST(
        tag = "dreamcast",
        displayName = "Sega Dreamcast",
        extensions = setOf(".gdi", ".cdi", ".chd", ".zip", ".7z"),
        screenScraperId = 23,
        defaultEmulators = listOf(
            EmulatorInfo("io.recompiled.redream", "Redream", null),
            EmulatorInfo("com.flycast.emulator", "Flycast", null),
            EmulatorInfo("com.reicast.emulator", "Reicast", null),
            EmulatorInfo("com.retroarch", "RetroArch", null),
            EmulatorInfo("com.retroarch.aarch64", "RetroArch (64-bit)", null),
        )
    ),

    PSX(
        tag = "psx",
        displayName = "PlayStation",
        extensions = setOf(".cue", ".pbp", ".chd", ".m3u", ".zip", ".7z"),
        screenScraperId = 57,
        defaultEmulators = listOf(
            EmulatorInfo("com.github.stenzek.duckstation", "DuckStation", null),
            EmulatorInfo("com.epsxe.ePSXe", "ePSXe", null),
            EmulatorInfo("com.emulator.fpse", "FPse", null),
            EmulatorInfo("com.emulator.fpse64", "FPse 64-bit", null),
            EmulatorInfo("com.retroarch", "RetroArch", null),
            EmulatorInfo("com.retroarch.aarch64", "RetroArch (64-bit)", null),
        )
    ),

    PS2(
        tag = "ps2",
        displayName = "PlayStation 2",
        extensions = setOf(".iso", ".chd", ".gz", ".cso", ".7z"),
        screenScraperId = 58,
        defaultEmulators = listOf(
            EmulatorInfo(
                "xyz.aethersx2.android",
                "NetherSX2",
                "https://github.com/Trixarian/NetherSX2-patch/releases"
            ),
            EmulatorInfo("com.retroarch", "RetroArch", null),
            EmulatorInfo("com.retroarch.aarch64", "RetroArch (64-bit)", null),
        )
    ),

    PSP(
        tag = "psp",
        displayName = "PlayStation Portable",
        extensions = setOf(".iso", ".cso", ".7z", ".zip"),
        screenScraperId = 61,
        defaultEmulators = listOf(
            EmulatorInfo("org.ppsspp.ppsspp", "PPSSPP", null),
            EmulatorInfo("org.ppsspp.ppssppgold", "PPSSPP Gold", null),
            EmulatorInfo("org.ppsspp.ppsspplegacy", "PPSSPP Legacy", null),
            EmulatorInfo("com.retroarch", "RetroArch", null),
            EmulatorInfo("com.retroarch.aarch64", "RetroArch (64-bit)", null),
        )
    ),

    GAMECUBE(
        tag = "gamecube",
        displayName = "Nintendo GameCube",
        extensions = setOf(".iso", ".gcm", ".gcz", ".ciso", ".rvz", ".7z"),
        screenScraperId = 21,
        defaultEmulators = listOf(
            EmulatorInfo("org.dolphinemu.dolphinemu", "Dolphin", null),
            EmulatorInfo("org.dolphinemu.mmjr", "Dolphin MMJR", null),
            EmulatorInfo("com.retroarch", "RetroArch", null),
            EmulatorInfo("com.retroarch.aarch64", "RetroArch (64-bit)", null),
        )
    ),

    WII(
        tag = "wii",
        displayName = "Nintendo Wii",
        extensions = setOf(".iso", ".wbfs", ".wad", ".gcz", ".ciso", ".rvz", ".7z"),
        screenScraperId = 36,
        defaultEmulators = listOf(
            EmulatorInfo("org.dolphinemu.dolphinemu", "Dolphin", null),
            EmulatorInfo("org.dolphinemu.mmjr", "Dolphin MMJR", null),
            EmulatorInfo("com.retroarch", "RetroArch", null),
            EmulatorInfo("com.retroarch.aarch64", "RetroArch (64-bit)", null),
        )
    ),

    NEOGEO(
        tag = "neogeo",
        displayName = "Neo Geo (AES/MVS)",
        extensions = setOf(".zip", ".neo", ".7z"),
        screenScraperId = 142,
        defaultEmulators = listOf(
            EmulatorInfo("com.explusalpha.NeoEmu", "NEO.emu", null),
            EmulatorInfo("com.seleuco.mame4d2024", "MAME4droid 2024", null),
            EmulatorInfo("com.retroarch", "RetroArch", null),
            EmulatorInfo("com.retroarch.aarch64", "RetroArch (64-bit)", null),
        )
    ),

    ARCADE(
        tag = "arcade",
        displayName = "Arcade",
        extensions = setOf(".zip", ".7z"),
        screenScraperId = 75,
        defaultEmulators = listOf(
            EmulatorInfo("com.seleuco.mame4d2024", "MAME4droid 2024", null),
            EmulatorInfo("com.retroarch", "RetroArch", null),
            EmulatorInfo("com.retroarch.aarch64", "RetroArch (64-bit)", null),
        )
    ),

    N3DS(
        tag = "3ds",
        displayName = "Nintendo 3DS",
        extensions = setOf(".3ds", ".cci", ".cxi", ".app", ".cia", ".zip", ".7z"),
        screenScraperId = 17,
        defaultEmulators = listOf(
            EmulatorInfo("org.citra.citra_emu", "Citra", null),
            EmulatorInfo("org.citra.citra_emu.canary", "Citra Canary", null),
            EmulatorInfo("io.github.lime3ds.android", "Lime3DS", "https://github.com/Lime3DS/Lime3DS/releases"),
            EmulatorInfo("org.panda3ds.pandroid", "Panda3DS", "https://github.com/wheremyfoodat/Panda3DS/releases"),
            EmulatorInfo("com.retroarch", "RetroArch", null),
            EmulatorInfo("com.retroarch.aarch64", "RetroArch (64-bit)", null),
        )
    ),

    SWITCH(
        tag = "switch",
        displayName = "Nintendo Switch",
        extensions = setOf(".nsp", ".xci", ".nca", ".nsz", ".xcz"),
        screenScraperId = 225,
        defaultEmulators = listOf(
            EmulatorInfo("org.yuzu.yuzu_emu", "Yuzu", "https://github.com/yuzu-mirror/yuzu-android/releases"),
            EmulatorInfo("dev.suyu.suyu_emu", "Suyu", "https://github.com/suyu-emu/suyu/releases"),
            EmulatorInfo("dev.eden.eden_emulator", "Eden", "https://eden-emu.dev/"),
            EmulatorInfo("dev.legacy.eden_emulator", "Eden (Legacy)", "https://eden-emu.dev/"),
            EmulatorInfo("dev.uzuy.edge", "Uzuy Edge", "https://github.com/uzuy-emu/uzuy/releases"),
            EmulatorInfo("skyline.emu", "Skyline", "https://github.com/skyline-emu/skyline/releases"),
        )
    );

    companion object {
        private val extensionMap: Map<String, List<Platform>> by lazy {
            val map = mutableMapOf<String, MutableList<Platform>>()
            entries.forEach { platform ->
                platform.extensions.forEach { ext ->
                    map.getOrPut(ext) { mutableListOf() }.add(platform)
                }
            }
            map
        }

        fun fromExtension(extension: String): List<Platform> {
            return extensionMap[extension.lowercase()] ?: emptyList()
        }

        fun fromTag(tag: String): Platform? {
            return entries.find { it.tag == tag }
        }
    }
}

/**
 * @param downloadUrl If non-null, this is a direct URL (e.g. GitHub releases) to download
 *                    the emulator when it's not on the Play Store.
 */
data class EmulatorInfo(
    val packageName: String,
    val displayName: String,
    val downloadUrl: String?
)
