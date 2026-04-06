package com.glyph.launcher.data.scanner

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.glyph.launcher.data.local.entity.GameEntity
import com.glyph.launcher.domain.model.Platform
import com.glyph.launcher.util.HashUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scans a user-selected directory tree (via SAF) to discover ROM files.
 * Supports both auto-detect mode and forced platform mode (per-folder assignment).
 */
@Singleton
class RomScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "RomScanner"
    }

    data class ScanResult(
        val discovered: List<GameEntity>,
        val skippedCount: Int,
        val totalScanned: Int
    )

    /**
     * Recursively scan a SAF directory tree for ROM files.
     *
     * @param rootUri The URI returned from ACTION_OPEN_DOCUMENT_TREE
     * @param forcePlatform If non-null, ALL files with valid extensions in this folder
     *                      are assigned to this platform (no guessing)
     * @param existingUris Set of file URIs already in the database (to skip duplicates)
     * @param onProgress Callback with (scannedSoFar, currentFileName)
     */
    suspend fun scan(
        rootUri: Uri,
        forcePlatform: Platform? = null,
        existingUris: Set<String> = emptySet(),
        onProgress: ((Int, String) -> Unit)? = null
    ): ScanResult = withContext(Dispatchers.IO) {
        android.util.Log.d(TAG, "Starting scan of: $rootUri with forcePlatform=$forcePlatform")
        val root = DocumentFile.fromTreeUri(context, rootUri)
            ?: return@withContext ScanResult(emptyList(), 0, 0)

        val discovered = mutableListOf<GameEntity>()
        val cueParentUris = mutableSetOf<String>()
        var totalScanned = 0
        var totalSkipped = 0

        scanRecursive(root, discovered, cueParentUris, existingUris, forcePlatform, onProgress) { t, s ->
            totalScanned += t; totalSkipped += s
        }

        ScanResult(
            discovered = discovered,
            skippedCount = totalSkipped,
            totalScanned = totalScanned
        )
    }

    private fun scanRecursive(
        dir: DocumentFile,
        results: MutableList<GameEntity>,
        cueParentUris: MutableSet<String>,
        existingUris: Set<String>,
        forcePlatform: Platform?,
        onProgress: ((Int, String) -> Unit)?,
        statsCallback: (Int, Int) -> Unit
    ) {
        var totalScanned = 0
        var skipped = 0
        val children = dir.listFiles()

        // First pass: collect .cue file locations
        for (file in children) {
            if (!file.isFile) continue
            val name = file.name ?: continue
            if (name.lowercase().endsWith(".cue")) {
                dir.uri.toString().let { cueParentUris.add(it) }
            }
        }

        // All valid extensions for the forced platform (if any)
        val forcedExtensions = forcePlatform?.extensions

        for (file in children) {
            if (file.isDirectory) {
                scanRecursive(file, results, cueParentUris, existingUris, forcePlatform, onProgress, statsCallback)
                continue
            }

            if (!file.isFile) continue
            val name = file.name ?: continue
            val uri = file.uri.toString()
            totalScanned++
            android.util.Log.d(TAG, "Checking file: $name ($uri)")

            onProgress?.invoke(results.size, name)

            // Skip if already in database
            if (uri in existingUris) {
                skipped++
                android.util.Log.d(TAG, "  -> Skipped (DUPLICATE)")
                continue
            }

            val extension = "." + name.substringAfterLast(".", "").lowercase()

            // Skip .bin files if a .cue exists in the same folder
            if (extension == ".bin" && dir.uri.toString() in cueParentUris) {
                skipped++
                android.util.Log.d(TAG, "  -> Skipped (.bin with .cue present)")
                continue
            }

            val platform: Platform

            if (forcePlatform != null) {
                // Forced mode: accept any file that matches the platform's extensions
                // OR accept common ROM archive extensions
                if (forcedExtensions != null && extension in forcedExtensions) {
                    platform = forcePlatform
                } else {
                    skipped++
                    android.util.Log.d(TAG, "  -> Skipped (Extension $extension not valid for forced platform $forcePlatform)")
                    continue
                }
            } else {
                // Auto-detect mode: map extension -> platform
                val platforms = Platform.fromExtension(extension)
                if (platforms.isEmpty()) {
                    skipped++
                    android.util.Log.d(TAG, "  -> Skipped (Unknown extension: $extension)")
                    continue
                }
                platform = if (platforms.size > 1) {
                    inferPlatformFromPath(dir, platforms) ?: platforms.first()
                } else {
                    platforms.first()
                }
            }

            val displayTitle = HashUtil.cleanFilename(name).ifBlank { name }

            results.add(
                GameEntity(
                    fileUri = uri,
                    filename = name,
                    displayTitle = displayTitle,
                    platformTag = platform.tag
                )
            )
            android.util.Log.d(TAG, "  -> ACCEPTED as ${platform.tag}")
        }

        statsCallback(totalScanned, skipped)
    }

    /**
     * Infer platform from parent folder name for ambiguous extensions.
     */
    private fun inferPlatformFromPath(dir: DocumentFile, candidates: List<Platform>): Platform? {
        val dirName = dir.name?.lowercase() ?: return null
        val normalizedDirName = dirName.replace(Regex("[^a-z0-9]"), "")

        return candidates.find { platform ->
            dirName.contains(platform.tag) ||
            normalizedDirName.contains(platform.tag) ||
            dirName.contains(platform.displayName.lowercase())
        }
    }
}
