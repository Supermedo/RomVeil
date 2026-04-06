package com.glyph.launcher.util

import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.StrictMode
import android.os.Environment
import android.provider.DocumentsContract
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import android.util.Log
import android.widget.Toast
import com.glyph.launcher.data.local.entity.GameEntity
import com.glyph.launcher.domain.model.EmulatorInfo
import com.glyph.launcher.domain.model.Platform

/**
 * Launches ROMs in emulators using the correct intent format for each emulator.
 *
 * Key: StrictMode VmPolicy is relaxed in GlyphApplication so we can use
 * file:// URIs, which is what ALL emulators expect. This is the same approach
 * used by Daijisho, JEROM, and other Android game launchers.
 */
object EmulatorLauncher {

    private const val TAG = "EmulatorLauncher"

    data class DetectionResult(
        val platform: Platform,
        val installed: List<EmulatorInfo>,
        val notInstalled: List<EmulatorInfo>
    )

    fun detectInstalledEmulators(context: Context, platform: Platform): DetectionResult {
        val pm = context.packageManager
        val installed = mutableListOf<EmulatorInfo>()
        val notInstalled = mutableListOf<EmulatorInfo>()

        platform.defaultEmulators.forEach { emulator ->
            if (isPackageInstalled(pm, emulator.packageName)) {
                installed.add(emulator)
            } else {
                notInstalled.add(emulator)
            }
        }

        return DetectionResult(platform, installed, notInstalled)
    }

    fun hasAnyEmulator(context: Context, platform: Platform): Boolean {
        return detectInstalledEmulators(context, platform).installed.isNotEmpty()
    }

    /** Check if a package is installed (for "· GET" chip: set default instead of opening store if now installed). */
    fun isEmulatorInstalled(context: Context, packageName: String): Boolean {
        return isPackageInstalled(context.packageManager, packageName)
    }

    fun openDownloadPage(context: Context, emulator: EmulatorInfo) {
        val url = emulator.downloadUrl
        if (url != null) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            } catch (_: Exception) { }
        }
        openPlayStore(context, emulator.packageName)
    }

    fun openPlayStore(context: Context, packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(context, "Could not open download page", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Launch a ROM in the specified emulator.
     * Resolves the SAF URI to a real path, then uses the appropriate
     * launch method for the specific emulator.
     */
    fun launchGame(
        context: Context,
        game: GameEntity,
        emulatorPackage: String
    ): Boolean {
        // HACK: Allow passing file:// URIs on Android 11+ (SDK 30+)
        // This is necessary because some emulators (Redream, Flycast, My Boy!)
        // have storage permission but fail when passed content:// URIs.
        // We must bypass the OS check to send them the raw file path they expect.
        try {
            val m = StrictMode::class.java.getMethod("disableDeathOnFileUriExposure")
            m.invoke(null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disable DeathOnFileUriExposure: ${e.message}")
        }

        val safUri = Uri.parse(game.fileUri)
        val realPath = resolveRealPath(safUri)
        val platform = Platform.fromTag(game.platformTag)

        Log.d(TAG, "Launching $emulatorPackage with safUri=$safUri path=$realPath")

        // ── Standard Emulators (PPSSPP, DuckStation, etc.) — Prefer File Path ──
        // Many emulators like PPSSPP actually prefer a real file path if available.
        // We only switch to Content URI if file path is missing or we are on Android 11+ AND the app specifically needs it.

        // PPSSPP: File path works great usually.
        if (emulatorPackage.startsWith("org.ppsspp")) {
            if (launchPPSSPP(context, safUri, realPath, emulatorPackage)) return true
        }

        // NetherSX2 / AetherSX2
        if (emulatorPackage == "xyz.aethersx2.android") {
            if (launchNetherSX2(context, safUri, realPath, emulatorPackage)) return true
        }

        // Dolphin (GameCube/Wii)
        if (isDolphin(emulatorPackage)) {
             // Dolphin often prefers content URI on modern Android, but let's try path first if that worked before
            val pathForDolphin = if (safUri.scheme != "content") realPath else null
            if (launchDolphin(context, safUri, pathForDolphin, emulatorPackage)) return true
        }

        // Dreamcast (Redream/Flycast/Reicast)
        // User reported Flycast WAS working before, so revert to standard path logic.
        // We do NOT treat it as special case anymore.

        // ── Problematic Emulators (Need Fixes) ──
        
        // NEO.emu / Snes9x EX+ / 2600.emu etc (Robert Broglia apps)
        // NEO.emu / Snes9x EX+ / 2600.emu etc (Robert Broglia apps)
        if (emulatorPackage.startsWith("com.explusalpha")) {
             // For NeoEmu, pass filename to handle .zip properly
             if (emulatorPackage == "com.explusalpha.NeoEmu") {
                 if (launchNeoEmu(context, safUri, realPath, game.filename, emulatorPackage)) return true
             } else {
                 // Snes9x, MD.emu, etc.
                 if (launchSnes9x(context, safUri, realPath, emulatorPackage)) return true
             }
        }


        
        if (emulatorPackage.startsWith("it.dbtecno")) {
             // Pizza Boy prefers SAF
             val contentUri = if (realPath != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                 getFileProviderUriForPath(context, realPath) ?: safUri
             } else safUri
             if (tryLaunchWithContentUriAndClipData(context, contentUri, emulatorPackage, game.filename)) return true
        }

        // DuckStation (both old and new packages)
        if (emulatorPackage.contains("duckstation")) {
             // DuckStation strongly prefers content URI (FileProvider) on Android 11+
             val contentUri = if (realPath != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                 getFileProviderUriForPath(context, realPath) ?: safUri
             } else safUri
             if (tryLaunchWithContentUriAndClipData(context, contentUri, emulatorPackage, game.filename)) return true
        }

        // Dreamcast (Redream/Flycast/Reicast) — FileProvider + file:// fallback
        if (platform == Platform.DREAMCAST && isDreamcastStandalone(emulatorPackage)) {
            // Pass realPath directly. Redream uses FileProvider. Flycast uses file:// path.
            if (launchDreamcastStandalone(context, safUri, realPath, emulatorPackage)) return true
        }

        // Mupen64Plus FZ (N64) — KEEP THIS FIX (User confirmed it works)
        if (emulatorPackage == "org.mupen64plusae.v3.fzurita") {
             // Try strict FileProvider URI first
             if (realPath != null) {
                 val fpUri = getFileProviderUriForPath(context, realPath)
                 if (fpUri != null && launchMupen64PlusFZ(context, fpUri, emulatorPackage)) return true
             }
             // Fallback to SAF URI
             if (safUri.scheme == "content" && launchMupen64PlusFZ(context, safUri, emulatorPackage)) return true
             // Legacy fallback
             if (realPath != null && launchMupen64PlusFZ(context, Uri.parse("file://$realPath"), emulatorPackage)) return true
        }

        // RetroArch — FIX: Prefer raw file path!
        // RetroArch typically has All Files Access or storage permissions, so file:// path is best.
        // content:// URIs often fail in RetroArch (black screen).
        if (emulatorPackage.startsWith("com.retroarch")) {
            // RetroArch usually has All Files Access, so prefer raw file path even on Android 11+.
            if (realPath != null) {
                 if (launchRetroArch(context, safUri, realPath, null, emulatorPackage, platform)) return true
            }
            
            // Fallback: Copy content URI to public dir if possible
            val publicPath = if (safUri.scheme == "content") {
                copyContentUriToPublicDir(context, safUri, game.filename)
            } else null
            
            if (publicPath != null) {
                 if (launchRetroArch(context, safUri, publicPath, null, emulatorPackage, platform)) return true
            }
            
            // Last resort: FileProvider
            val fpUri = if (realPath != null) getFileProviderUriForPath(context, realPath) else null
            if (launchRetroArch(context, safUri, null, fpUri, emulatorPackage, platform)) return true
        } else {
             // Generic fallbacks for unhandled emulators
        }

        // ── Generic Launch Strategies ──
        
        // 1. Try raw file path (standard behavior)
        if (realPath != null) {
            if (launchWithFileView(context, realPath, emulatorPackage)) return true
        }

        // 2. Try SAF URI
        if (tryLaunchWithSafUri(context, safUri, game.filename, emulatorPackage)) return true
        
        // 3. Last resort: FileProvider URI (Android 11+ fix for generic apps)
        if (realPath != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val fpUri = getFileProviderUriForPath(context, realPath)
            if (fpUri != null) {
                if (tryLaunchWithContentUriAndClipData(context, fpUri, emulatorPackage, game.filename)) return true
            }
        }

        Log.w(TAG, "All launch strategies failed for $emulatorPackage")
        Toast.makeText(context, "Could not load ROM — opening emulator", Toast.LENGTH_LONG).show()
        return tryOpenEmulator(context, emulatorPackage)
    }

    /**
     * Try with SAF content:// URI and read permission grant.
     * Works for emulators that support ContentResolver (PPSSPP, some others).
     */
    private fun tryLaunchWithSafUri(
        context: Context,
        safUri: Uri,
        filename: String,
        emulatorPackage: String
    ): Boolean {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(safUri, "application/octet-stream")
                setPackage(emulatorPackage)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
            return true
        } catch (e: Exception) {
            Log.d(TAG, "SAF URI launch failed: ${e.message}")
        }
        return false
    }

    /**
     * Try launching with a content URI (e.g. FileProvider) and read permission.
     * Used for emulators that don't resolve SAF URIs but can read content URIs we grant.
     */
    private fun tryLaunchWithContentUri(
        context: Context,
        contentUri: Uri,
        emulatorPackage: String
    ): Boolean {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/octet-stream")
                setPackage(emulatorPackage)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
            return true
        } catch (e: Exception) {
            Log.d(TAG, "Content URI launch failed for $emulatorPackage: ${e.message}")
        }
        return false
    }

    /**
     * Launch with content URI + ClipData + grant permission. Tries platform-specific MIME first so
     * emulator intent-filters match (e.g. application/x-nes-rom for NES.emu).
     */
    private fun tryLaunchWithContentUriAndClipData(
        context: Context,
        contentUri: Uri,
        emulatorPackage: String,
        filename: String
    ): Boolean {
        val ext = filename.substringAfterLast(".", "").take(4)
        val mimeTypes = listOf(getMimeType(ext), "application/octet-stream", "*/*")
        for (mime in mimeTypes.distinct()) {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(contentUri, mime)
                    setPackage(emulatorPackage)
                    clipData = ClipData.newRawUri("", contentUri)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                Log.d(TAG, "Content URI + ClipData ($mime) failed for $emulatorPackage: ${e.message}")
            }
        }
        return false
    }

    /**
     * Mupen64Plus FZ / M64+ FZ: launch with ROM URI. Tries SplashActivity first, then VIEW with setPackage (let app pick activity).
     */
    private fun launchMupen64PlusFZ(
        context: Context,
        romUri: Uri,
        emulatorPackage: String
    ): Boolean {
        // 1) SplashActivity with URI (required for game to load in many builds)
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                component = ComponentName(emulatorPackage, "paulscode.android.mupen64plusae.SplashActivity")
                setDataAndType(romUri, "application/octet-stream")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = ClipData.newRawUri("", romUri)
            }
            context.startActivity(intent)
            return true
        } catch (e: Exception) {
            Log.d(TAG, "Mupen64Plus FZ SplashActivity failed: ${e.message}")
        }
        // 2) VIEW with setPackage only (let app resolve which activity handles the URI)
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(romUri, "application/octet-stream")
                setPackage(emulatorPackage)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
            return true
        } catch (e: Exception) {
            Log.d(TAG, "Mupen64Plus FZ VIEW failed: ${e.message}")
        }
        return false
    }

    // ── Emulator-Specific Launch Strategies ──────────────────────────────────

    /**
     * Standard launch: ACTION_VIEW with file:// URI.
     * Works for: DuckStation, PPSSPP, Redream, Flycast, Reicast, MD.emu,
     *            NES.emu, Snes9x EX+, GBA.emu, GBC.emu, DraStic,
     *            Mupen64Plus FZ, NetherSX2/AetherSX2, Yaba Sanshiro, etc.
     */
    private fun launchWithFileView(
        context: Context,
        filePath: String,
        emulatorPackage: String
    ): Boolean {
        val fileUri = Uri.parse("file://$filePath")

        // Try with specific MIME type
        try {
            val ext = filePath.substringAfterLast(".")
            val mime = getMimeType(ext)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(fileUri, mime)
                setPackage(emulatorPackage)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
            return true
        } catch (e: Exception) {
            Log.d(TAG, "Launch with MIME failed: ${e.message}")
        }

        // Try with generic MIME
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(fileUri, "application/octet-stream")
                setPackage(emulatorPackage)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
            return true
        } catch (e: Exception) {
            Log.d(TAG, "Launch with generic MIME failed: ${e.message}")
        }

        // Try with no MIME type at all
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = fileUri
                setPackage(emulatorPackage)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
            return true
        } catch (e: Exception) {
            Log.d(TAG, "Launch with no MIME failed: ${e.message}")
        }

        // Try with wildcard MIME
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(fileUri, "*/*")
                setPackage(emulatorPackage)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
            return true
        } catch (e: Exception) {
            Log.d(TAG, "Launch with wildcard MIME failed: ${e.message}")
        }

        return false
    }

    /**
     * Snes9x EX+: on Android 10+ passing file path causes "no permission". Use content URI with
     * FLAG_GRANT_READ_URI_PERMISSION so the emulator can read the file.
     */
    private fun launchSnes9x(
        context: Context,
        contentUri: Uri,
        filePath: String?,
        emulatorPackage: String
    ): Boolean {
        if (contentUri.scheme == "content") {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(contentUri, "application/octet-stream")
                    setPackage(emulatorPackage)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                Log.d(TAG, "Snes9x content URI launch failed: ${e.message}")
            }
        }
        // file:// not readable by other apps on Android 11+
        if (filePath != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.R && launchWithFileView(context, filePath, emulatorPackage)) return true
        return false
    }

    /**
     * NEO.emu: like Snes9x, needs content URI with FLAG_GRANT_READ_URI_PERMISSION so the app can read the ROM.
     * Tries application/zip first for .zip Neo Geo ROMs so the intent filter matches.
     */
    private fun launchNeoEmu(
        context: Context,
        contentUri: Uri,
        filePath: String?,
        filename: String,
        emulatorPackage: String
    ): Boolean {
        val ext = filename.substringAfterLast(".", "").take(4)
        val mimeForZip = if (ext.equals("zip", ignoreCase = true)) "application/zip" else null

        // 1) Content URI with permission grant — try application/zip first for .zip, then octet-stream
        if (contentUri.scheme == "content") {
            val mimes = listOfNotNull(mimeForZip, "application/octet-stream", "*/*").distinct()
            for (mime in mimes) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(contentUri, mime)
                        setPackage(emulatorPackage)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        clipData = ClipData.newRawUri("", contentUri)
                    }
                    context.startActivity(intent)
                    return true
                } catch (e: Exception) {
                    Log.d(TAG, "NEO.emu content URI ($mime) failed: ${e.message}")
                }
            }
        }

        // 2) Android 11+: FileProvider URI for path so NEO.emu can read via ContentResolver
        if (filePath != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val uriForPath = getFileProviderUriForPath(context, filePath)
            if (uriForPath != null) {
                val mimes = listOfNotNull(mimeForZip, "application/octet-stream").distinct()
                for (mime in mimes) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uriForPath, mime)
                            setPackage(emulatorPackage)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            clipData = ClipData.newRawUri("", uriForPath)
                        }
                        context.startActivity(intent)
                        return true
                    } catch (e: Exception) {
                        Log.d(TAG, "NEO.emu FileProvider URI ($mime) failed: ${e.message}")
                    }
                }
            }
        }

        // 3) Copy to public path and try content URI for that (in case SAF URI wasn't accepted)
        if (contentUri.scheme == "content") {
            val publicPath = copyContentUriToPublicDir(context, contentUri, filename)
            if (publicPath != null) {
                val uriForPath = getFileProviderUriForPath(context, publicPath)
                if (uriForPath != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uriForPath, mimeForZip ?: "application/octet-stream")
                            setPackage(emulatorPackage)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            clipData = ClipData.newRawUri("", uriForPath)
                        }
                        context.startActivity(intent)
                        return true
                    } catch (e: Exception) {
                        Log.d(TAG, "NEO.emu public path FileProvider failed: ${e.message}")
                    }
                }
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R && launchWithFileView(context, publicPath, emulatorPackage)) return true
            }
        }

        // 4) File path fallback (pre-Android-11)
        if (filePath != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.R && launchWithFileView(context, filePath, emulatorPackage)) return true
        return false
    }

    /**
     * PPSSPP: must use PpssppActivity with ACTION_VIEW and intent data = game URI.
     * Using launcher activity or setPackage only opens the app without loading the game.
     */
    private fun launchPPSSPP(
        context: Context,
        contentUri: Uri,
        filePath: String?,
        emulatorPackage: String
    ): Boolean {
        // Activity class is same for free/gold/legacy; package differs
        val activityClass = "org.ppsspp.ppsspp.PpssppActivity"

        // Content URI first (required on Android 11+; file:// not readable by other apps)
        if (contentUri.scheme == "content") {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    component = ComponentName(emulatorPackage, activityClass)
                    setDataAndType(contentUri, "application/octet-stream")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                Log.d(TAG, "PPSSPP content URI launch failed: ${e.message}")
            }
        }

        // Android 11+: use FileProvider content URI for path so PPSSPP can read the file
        if (filePath != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val uriForPath = getFileProviderUriForPath(context, filePath)
            if (uriForPath != null) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        component = ComponentName(emulatorPackage, activityClass)
                        setDataAndType(uriForPath, "application/octet-stream")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    context.startActivity(intent)
                    return true
                } catch (e: Exception) {
                    Log.d(TAG, "PPSSPP FileProvider URI launch failed: ${e.message}")
                }
            }
        }

        // Pre-Android-11 fallback: file path
        if (filePath != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            try {
                val fileUri = Uri.parse("file://$filePath")
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    component = ComponentName(emulatorPackage, activityClass)
                    setDataAndType(fileUri, "application/octet-stream")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                Log.d(TAG, "PPSSPP file path launch failed: ${e.message}")
            }
        }
        return false
    }

    private fun isDolphin(packageName: String): Boolean =
        packageName == "org.dolphinemu.dolphinemu" || packageName == "org.dolphinemu.mmjr"

    /**
     * Dolphin (GameCube/Wii) prioritizes content URI (intent.data) over AutoStartFile path.
     * Pass content URI with FLAG_GRANT_READ_URI_PERMISSION so Dolphin opens the same file as when user picks it manually.
     */
    private fun launchDolphin(
        context: Context,
        contentUri: Uri,
        filePath: String?,
        emulatorPackage: String
    ): Boolean {
        val mainActivityClass = when (emulatorPackage) {
            "org.dolphinemu.mmjr" -> "org.dolphinemu.mmjr.ui.main.MainActivity"
            else -> "org.dolphinemu.dolphinemu.ui.main.MainActivity"
        }
        // Method 1: Content URI (required on Android 11+; Dolphin can read via ContentResolver)
        if (contentUri.scheme == "content") {
            try {
                val intent = Intent().apply {
                    component = ComponentName(emulatorPackage, mainActivityClass)
                    data = contentUri
                    clipData = ClipData.newRawUri("", contentUri)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                Log.d(TAG, "Dolphin content URI launch failed: ${e.message}")
            }
        }
        // Method 2: Android 11+ with file path — use FileProvider content URI so Dolphin can read
        if (filePath != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val uriForPath = getFileProviderUriForPath(context, filePath)
            if (uriForPath != null) {
                try {
                    val intent = Intent().apply {
                        component = ComponentName(emulatorPackage, mainActivityClass)
                        data = uriForPath
                        clipData = ClipData.newRawUri("", uriForPath)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                    return true
                } catch (e: Exception) {
                    Log.d(TAG, "Dolphin FileProvider URI launch failed: ${e.message}")
                }
            }
        }
        // Method 3: Pre-Android-11 — AutoStartFile path or file:// VIEW
        if (filePath != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            try {
                val intent = Intent().apply {
                    component = ComponentName(emulatorPackage, mainActivityClass)
                    putExtra("AutoStartFile", filePath)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                Log.d(TAG, "Dolphin AutoStartFile launch failed: ${e.message}")
            }
            if (launchWithFileView(context, filePath, emulatorPackage)) return true
        }
        return false
    }

    private fun isDreamcastStandalone(packageName: String): Boolean =
        packageName == "io.recompiled.redream" ||
        packageName == "com.flycast.emulator" ||
        packageName == "com.reicast.emulator"

    /**
     * Redream / Flycast / Reicast: prefer content URI so game loads on Android 10+ (scoped storage).
     * File path often opens the app but doesn't load the game.
     */




    private fun launchDreamcastStandalone(
        context: Context,
        contentUri: Uri,
        filePath: String?,
        emulatorPackage: String
    ): Boolean {
        val activityByPackage = when (emulatorPackage) {
            "com.flycast.emulator" -> "com.flycast.emulator.MainActivity"
            "com.reicast.emulator" -> "com.reicast.emulator.MainActivity"
            "io.recompiled.redream" -> "io.recompiled.redream.MainActivity" 
            else -> null
        }

        Log.d(TAG, "launchDreamcastStandalone: filePath=$filePath, contentUri=$contentUri, pkg=$emulatorPackage")

        // Redream: Confirmed working with FileProvider
        if (emulatorPackage == "io.recompiled.redream" && filePath != null) {
             try {
                val file = File(filePath)
                if (file.exists()) {
                    val fpUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(fpUri, "*/*")
                        setPackage(emulatorPackage)
                        component = ComponentName(emulatorPackage, activityByPackage!!)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                    Log.d(TAG, "Redream: launched with FileProvider URI: $fpUri")
                    return true
                }
            } catch (e: Exception) {
                Log.d(TAG, "Redream FileProvider launch failed: ${e.message}")
            }
        }

        // Flycast: Uses SAF (confirmed via screenshot). PRIORITIZE SAF URI.
        if (emulatorPackage.contains("flycast") || emulatorPackage.contains("reicast")) {
            // Strategy 1: SAF content:// URI (Highest success chance for SAF-aware apps)
            if (contentUri.scheme == "content") {
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(contentUri, "application/octet-stream") // Try generic mime
                        setPackage(emulatorPackage)
                        if (activityByPackage != null) {
                            component = ComponentName(emulatorPackage, activityByPackage)
                        }
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                    Log.d(TAG, "Flycast: launched with SAF URI: $contentUri")
                    return true
                } catch (e: Exception) {
                    Log.d(TAG, "Flycast SAF URI launch failed: ${e.message}")
                }
            }
        }

        // Fallback Strategies (for older versions or if SAF fails)

        // Strategy: Raw File Path (No Scheme) — Fixed 'Cannot stat file://' error
        if (filePath != null) {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(filePath), "*/*")
                    setPackage(emulatorPackage)
                    if (activityByPackage != null) {
                        component = ComponentName(emulatorPackage, activityByPackage)
                    }
                    putExtra("rom", filePath)
                    putExtra("path", filePath)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                context.startActivity(intent)
                Log.d(TAG, "Dreamcast: launched with RAW PATH URI")
                return true
            } catch (e: Exception) {
                Log.d(TAG, "Dreamcast raw path launch failed: ${e.message}")
            }
        }
        
        // Strategy: standard file:// URI
        if (filePath != null) {
            try {
                 val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse("file://" + filePath), "*/*")
                    setPackage(emulatorPackage)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                context.startActivity(intent)
                return true
            } catch (e: Exception) {}
        }

        return false
    }

    /**
     * NetherSX2/AetherSX2: launch via EmulationActivity with bootPath (content URI).
     * On Android 11+, use FileProvider content URI when we only have file path so the emulator can read the file.
     */
    private fun launchNetherSX2(
        context: Context,
        contentUri: Uri,
        filePath: String?,
        emulatorPackage: String
    ): Boolean {
        val uriToUse = when {
            contentUri.scheme == "content" -> contentUri
            filePath != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> getFileProviderUriForPath(context, filePath)
            else -> contentUri
        } ?: contentUri
        return try {
            val intent = Intent().apply {
                component = ComponentName(
                    emulatorPackage,
                    "xyz.aethersx2.android.EmulationActivity"
                )
                putExtra("bootPath", uriToUse.toString())
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                data = uriToUse
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.d(TAG, "NetherSX2 EmulationActivity failed: ${e.message}")
            false
        }
    }

    /**
     * RetroArch core .so filename (without path) per platform. User must have core installed.
     * First name is primary; fallbacks used if primary not found (e.g. different RetroArch build).
     */
    private fun getRetroArchCoreNames(platform: Platform?): List<String> = when (platform) {
        Platform.NES -> listOf("fceumm_libretro_android.so")
        Platform.SNES -> listOf("snes9x_libretro_android.so", "snes9x_next_libretro_android.so", "bsnes_mercury_accuracy_libretro_android.so")
        Platform.N64 -> listOf("mupen64plus_next_gles3_libretro_android.so", "mupen64plus_next_libretro_android.so", "parallel_n64_libretro_android.so")
        Platform.GB, Platform.GBC -> listOf("gambatte_libretro_android.so", "sameboy_libretro_android.so")
        Platform.GBA -> listOf("mgba_libretro_android.so", "gpsp_libretro_android.so", "vba_next_libretro_android.so")
        Platform.NDS -> listOf("desmume2015_libretro_android.so", "melonds_libretro_android.so")
        Platform.GENESIS -> listOf("genesis_plus_gx_libretro_android.so", "picodrive_libretro_android.so")
        Platform.SEGA32X -> listOf("picodrive_libretro_android.so")
        Platform.SATURN -> listOf("yabause_libretro_android.so", "beetle_saturn_libretro_android.so")
        Platform.DREAMCAST -> listOf("flycast_libretro_android.so", "flycast_gles2_libretro_android.so")
        Platform.PSX -> listOf("swanstation_libretro_android.so", "pcsx_rearmed_libretro_android.so", "beetle_psx_libretro_android.so")
        Platform.PS2 -> emptyList()
        Platform.PSP -> listOf("ppsspp_libretro_android.so")
        Platform.GAMECUBE, Platform.WII -> emptyList() // Dolphin standalone, no RetroArch core
        Platform.NEOGEO -> listOf("fbneo_libretro_android.so", "mame2003_plus_libretro_android.so")
        Platform.ARCADE -> listOf("mame2003_plus_libretro_android.so", "fbneo_libretro_android.so")
        else -> emptyList()
    }

    /**
     * RetroArch-specific launch. Tries safe methods first (ACTION_VIEW) to avoid crashes.
     * RetroActivityFuture with a non-existent LIBRETRO path can crash; try without LIBRETRO first.
     */
    private fun launchRetroArch(
        context: Context,
        contentUri: Uri,
        filePath: String?,
        fileProviderUri: Uri?,
        retroarchPackage: String,
        platform: Platform?
    ): Boolean {
        val coreNames = getRetroArchCoreNames(platform)
        
        // Construct full core paths assuming standard install location
        val libretroPaths = coreNames.map { "/data/user/0/$retroarchPackage/cores/$it" }
        // Try paths: first with explicit core, then with no core (let RA detect)
        val pathsToTry = if (libretroPaths.isEmpty()) listOf<String?>(null) else libretroPaths + listOf<String?>(null)

        // 1. Try RetroActivityFuture with RAW FILE PATH (Works best if RA has storage permission)
        if (filePath != null) {
            for (libretroPath in pathsToTry) {
                try {
                    val intent = Intent(Intent.ACTION_MAIN).apply {
                        component = ComponentName(retroarchPackage, "com.retroarch.browser.retroactivity.RetroActivityFuture")
                        putExtra("ROM", filePath)
                        libretroPath?.let { putExtra("LIBRETRO", it) }
                        putExtra("CONFIGFILE", "/storage/emulated/0/Android/data/$retroarchPackage/files/retroarch.cfg")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    context.startActivity(intent)
                    return true
                } catch (e: Exception) {
                     Log.d(TAG, "RetroArch future launch failed for $libretroPath: ${e.message}")
                }
            }
             // 1b. Try standard ACTION_VIEW with file path
             try {
                 val intent = Intent(Intent.ACTION_VIEW).apply {
                     setDataAndType(Uri.fromFile(File(filePath)), "application/octet-stream")
                     setPackage(retroarchPackage)
                     putExtra("ROM", filePath)
                     addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                     addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                 }
                 context.startActivity(intent)
                 return true
             } catch (e: Exception) { }
        }

        // 2. Try FileProvider URI (Android 11+ fallback)
        val uriToUse = fileProviderUri ?: (if (contentUri.scheme == "content") contentUri else null)
        if (uriToUse != null) {
             try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uriToUse, "application/octet-stream")
                    setPackage(retroarchPackage)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
                return true
             } catch (e: Exception) { }
        }

        return false
    }

    /**
     * Just open the emulator app without loading a ROM.
     */
    private fun tryOpenEmulator(context: Context, emulatorPackage: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(emulatorPackage)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else false
        } catch (_: Exception) {
            false
        }
    }

    // ── Copy content URI to a path emulators can read ───────────────────────

    /**
     * Copy ROM from content URI to a public external directory (Download/RetroS or Documents/RetroS).
     * RetroArch can read these paths if it has storage permission. Returns the absolute file path, or null on failure.
     */
    private fun copyContentUriToPublicDir(context: Context, contentUri: Uri, filename: String): String? {
        val dirsToTry = listOf(
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "RetroS"),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "RetroS")
        )
        for (publicDir in dirsToTry) {
            try {
                if (!publicDir.exists()) publicDir.mkdirs()
                if (!publicDir.exists()) continue
                val ext = filename.substringAfterLast(".", "bin").take(4)
                val outFile = File(publicDir, "rom_${System.currentTimeMillis()}.$ext")
                context.contentResolver.openInputStream(contentUri)?.use { input ->
                    FileOutputStream(outFile).use { output ->
                        input.copyTo(output)
                    }
                }
                return outFile.absolutePath
            } catch (e: Exception) {
                Log.d(TAG, "Copy to ${publicDir.absolutePath} failed: ${e.message}")
            }
        }
        return null
    }

    /**
     * Copy ROM to a shared public directory (Download/RetroS) keeping the original filename.
     * This allows emulators without MANAGE_EXTERNAL_STORAGE to read the file.
     * Reuses existing copy if file already exists with same size.
     */
    private fun copyRomToSharedDir(context: Context, safUri: Uri, realPath: String?, filename: String): String? {
        val publicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "RetroS")
        try {
            if (!publicDir.exists()) publicDir.mkdirs()
            if (!publicDir.exists()) return null

            val outFile = File(publicDir, filename)

            // If file already exists, check if it's the same size (reuse copy)
            if (outFile.exists()) {
                // Quick check: if source is a real file, compare sizes
                if (realPath != null) {
                    val srcFile = File(realPath)
                    if (srcFile.exists() && srcFile.length() == outFile.length()) {
                        Log.d(TAG, "Reusing existing copy: ${outFile.absolutePath}")
                        return outFile.absolutePath
                    }
                } else {
                    // If we can't compare, just reuse it
                    Log.d(TAG, "Reusing existing copy (no size check): ${outFile.absolutePath}")
                    return outFile.absolutePath
                }
            }

            // Copy from real path if available (faster, no content resolver needed)
            if (realPath != null) {
                val srcFile = File(realPath)
                if (srcFile.exists() && srcFile.canRead()) {
                    srcFile.inputStream().use { input ->
                        FileOutputStream(outFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    Log.d(TAG, "Copied ROM to: ${outFile.absolutePath}")
                    return outFile.absolutePath
                }
            }

            // Copy from SAF content URI
            if (safUri.scheme == "content") {
                context.contentResolver.openInputStream(safUri)?.use { input ->
                    FileOutputStream(outFile).use { output ->
                        input.copyTo(output)
                    }
                }
                Log.d(TAG, "Copied ROM (via SAF) to: ${outFile.absolutePath}")
                return outFile.absolutePath
            }
        } catch (e: Exception) {
            Log.e(TAG, "copyRomToSharedDir failed: ${e.message}")
        }
        return null
    }

    /**
     * Copy ROM to app cache and return a content URI via FileProvider. RetroArch can read this with FLAG_GRANT_READ_URI_PERMISSION.
     */
    private fun getContentUriViaFileProvider(context: Context, contentUri: Uri, filename: String): Uri? {
        return try {
            val outFile = copyContentUriToCacheFile(context, contentUri, filename) ?: return null
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
        } catch (e: Exception) {
            Log.d(TAG, "FileProvider URI failed: ${e.message}")
            null
        }
    }

    /** Content URI for an existing file path (e.g. Download/RetroS). Avoids file:// which can crash target apps on Android 7+. */
    private fun getFileProviderUriForPath(context: Context, filePath: String): Uri? {
        return try {
            val file = File(filePath)
            if (!file.canRead()) return null
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            Log.d(TAG, "FileProvider URI for path failed: ${e.message}")
            null
        }
    }

    /** Copy to cache and return the File (for FileProvider). */
    private fun copyContentUriToCacheFile(context: Context, contentUri: Uri, filename: String): File? {
        return try {
            val cacheDir = File(context.cacheDir, "rom_launch").apply { if (!exists()) mkdirs() }
            val ext = filename.substringAfterLast(".", "bin").take(4)
            val outFile = File(cacheDir, "rom_${System.currentTimeMillis()}.$ext")
            context.contentResolver.openInputStream(contentUri)?.use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
            outFile
        } catch (e: Exception) {
            Log.d(TAG, "Copy content URI to cache failed: ${e.message}")
            null
        }
    }

    private fun copyContentUriToCache(context: Context, contentUri: Uri, filename: String): String? {
        return copyContentUriToCacheFile(context, contentUri, filename)?.absolutePath
    }

    // ── SAF URI to Real Path Resolution ──────────────────────────────────────

    /**
     * Resolve a SAF content:// URI to a real filesystem path.
     * Works for internal storage and SD cards.
     */
    fun resolveRealPath(uri: Uri): String? {
        if (uri.scheme == "file") {
            return uri.path
        }

        if (uri.authority == "com.android.externalstorage.documents") {
            return resolveExternalStoragePath(uri)
        }

        // For other content providers, try to extract path from URI
        val path = uri.path
        if (path != null && path.startsWith("/storage/")) {
            return path
        }

        return null
    }

    private fun resolveExternalStoragePath(uri: Uri): String? {
        try {
            val docId = extractDocumentId(uri) ?: return null

            if (docId.contains(":")) {
                val colonIndex = docId.indexOf(":")
                val type = docId.substring(0, colonIndex)
                val relativePath = docId.substring(colonIndex + 1)

                return when {
                    "primary".equals(type, ignoreCase = true) -> {
                        "${Environment.getExternalStorageDirectory().absolutePath}/$relativePath"
                    }
                    else -> {
                        "/storage/$type/$relativePath"
                    }
                }
            }
        } catch (_: Exception) { }
        return null
    }

    private fun extractDocumentId(uri: Uri): String? {
        // Try standard API
        try {
            if (DocumentsContract.isDocumentUri(null, uri)) {
                return DocumentsContract.getDocumentId(uri)
            }
        } catch (_: Exception) { }

        // Parse from encoded path: .../document/<docId>
        val path = uri.encodedPath ?: return null
        val docSegment = "/document/"
        val docIndex = path.lastIndexOf(docSegment)
        if (docIndex >= 0) {
            return Uri.decode(path.substring(docIndex + docSegment.length))
        }

        // Parse from tree path
        val treeSegment = "/tree/"
        val treeIndex = path.indexOf(treeSegment)
        if (treeIndex >= 0) {
            return Uri.decode(path.substring(treeIndex + treeSegment.length))
        }

        return null
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun isPackageInstalled(pm: PackageManager, packageName: String): Boolean {
        return try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun getMimeType(ext: String): String = when (ext.lowercase()) {
        "sfc", "smc" -> "application/x-snes-rom"
        "nes" -> "application/x-nes-rom"
        "gba" -> "application/x-gba-rom"
        "gb", "gbc" -> "application/x-gameboy-rom"
        "n64", "z64", "v64" -> "application/x-n64-rom"
        "nds" -> "application/x-nintendo-ds-rom"
        "gen", "md", "smd" -> "application/x-genesis-rom"
        "zip" -> "application/zip"
        "neo" -> "application/octet-stream"
        else -> "application/octet-stream"
    }
}
