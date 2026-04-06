package com.glyph.launcher.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a discovered ROM and its scraped metadata.
 */
@Entity(
    tableName = "games",
    indices = [
        Index(value = ["file_uri"], unique = true),
        Index(value = ["platform_tag"])
    ]
)
data class GameEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "game_id")
    val gameId: Long = 0,

    @ColumnInfo(name = "file_uri")
    val fileUri: String,

    @ColumnInfo(name = "filename")
    val filename: String,

    @ColumnInfo(name = "display_title")
    val displayTitle: String,

    @ColumnInfo(name = "platform_tag")
    val platformTag: String,

    @ColumnInfo(name = "release_date")
    val releaseDate: String? = null,

    @ColumnInfo(name = "developer")
    val developer: String? = null,

    @ColumnInfo(name = "genre")
    val genre: String? = null,

    @ColumnInfo(name = "background_image_path")
    val backgroundImagePath: String? = null,

    @ColumnInfo(name = "preferred_emulator_package")
    val preferredEmulatorPackage: String? = null,

    @ColumnInfo(name = "scrape_status")
    val scrapeStatus: ScrapeStatus = ScrapeStatus.PENDING,

    @ColumnInfo(name = "last_played")
    val lastPlayed: Long? = null,

    @ColumnInfo(name = "play_count")
    val playCount: Int = 0,

    /** Star rating 0–5 from scraper (e.g. RAWG). Null if not scraped or unavailable. */
    @ColumnInfo(name = "rating")
    val rating: Float? = null,

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false
)

enum class ScrapeStatus {
    PENDING,
    SUCCESS,
    FAILED,
    SKIPPED
}
