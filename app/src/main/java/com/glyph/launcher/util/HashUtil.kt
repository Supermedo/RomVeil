package com.glyph.launcher.util

import android.content.Context
import android.net.Uri
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.CRC32

/**
 * Utility for cleaning ROM filenames and computing file hashes.
 */
object HashUtil {

    /**
     * Compute MD5 hash of a file identified by a content URI.
     * Returns lowercase hex string.
     */
    suspend fun computeMd5(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                computeMd5(stream)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun computeMd5(inputStream: InputStream): String {
        val digest = MessageDigest.getInstance("MD5")
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Compute CRC32 of a file identified by a content URI.
     * Returns hex string.
     */
    suspend fun computeCrc32(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                computeCrc32(stream)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun computeCrc32(inputStream: InputStream): String {
        val crc = CRC32()
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            crc.update(buffer, 0, bytesRead)
        }
        return "%08x".format(crc.value)
    }

    /**
     * Get the file size from a content URI.
     */
    fun getFileSize(context: Context, uri: Uri): Long? {
        return try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use {
                it.length
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Clean a ROM filename for fallback text search.
     * Removes common tags like (USA), [!], (Rev 1), etc.
     */
    fun cleanFilename(filename: String): String {
        return filename
            .substringBeforeLast(".")                // remove extension
            .replace(".", " ")                       // replace dots with spaces (e.g. scene releases)
            .replace(Regex("\\(.*?\\)"), "")         // remove (parenthesized) tags
            .replace(Regex("\\[.*?]"), "")          // remove [bracketed] tags
            .replace(Regex("[_\\-]+"), " ")         // normalize separators
            .replace(Regex("\\s+"), " ")            // collapse whitespace
            .trim()
    }
}
