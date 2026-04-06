package com.glyph.launcher.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.glyph.launcher.data.local.entity.GameEntity
import com.glyph.launcher.data.local.entity.ScrapeStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {

    // ── Queries ──────────────────────────────────────────────────────────────

    @Query("SELECT * FROM games ORDER BY display_title ASC")
    fun getAllGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE platform_tag = :platformTag ORDER BY display_title ASC")
    fun getGamesByPlatform(platformTag: String): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE is_favorite = 1 ORDER BY display_title ASC")
    fun getFavoriteGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE game_id = :gameId")
    suspend fun getGameById(gameId: Long): GameEntity?

    @Query("SELECT * FROM games WHERE file_uri = :fileUri LIMIT 1")
    suspend fun getGameByUri(fileUri: String): GameEntity?

    @Query("SELECT * FROM games WHERE scrape_status = :status")
    suspend fun getGamesByScrapeStatus(status: ScrapeStatus): List<GameEntity>

    @Query("SELECT DISTINCT platform_tag FROM games ORDER BY platform_tag ASC")
    fun getDistinctPlatforms(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM games")
    suspend fun getGameCount(): Int

    @Query("SELECT * FROM games ORDER BY last_played DESC LIMIT :limit")
    fun getRecentlyPlayed(limit: Int = 10): Flow<List<GameEntity>>

    // ── Mutations ────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGame(game: GameEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGames(games: List<GameEntity>): List<Long>

    @Update
    suspend fun updateGame(game: GameEntity)

    @Query("""
        UPDATE games SET 
            display_title = :displayTitle,
            release_date = :releaseDate,
            developer = :developer,
            genre = :genre,
            background_image_path = :backgroundImagePath,
            scrape_status = :scrapeStatus,
            rating = :rating
        WHERE game_id = :gameId
    """)
    suspend fun updateScrapedData(
        gameId: Long,
        displayTitle: String,
        releaseDate: String?,
        developer: String?,
        genre: String?,
        backgroundImagePath: String?,
        scrapeStatus: ScrapeStatus,
        rating: Float?
    )

    @Query("UPDATE games SET preferred_emulator_package = :packageName WHERE game_id = :gameId")
    suspend fun setPreferredEmulator(gameId: Long, packageName: String)

    @Query("UPDATE games SET last_played = :timestamp, play_count = play_count + 1 WHERE game_id = :gameId")
    suspend fun recordPlay(gameId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE games SET background_image_path = :path WHERE game_id = :gameId")
    suspend fun updateBackgroundImage(gameId: Long, path: String?)

    @Query("UPDATE games SET display_title = :title WHERE game_id = :gameId")
    suspend fun updateGameTitle(gameId: Long, title: String)

    @Query("UPDATE games SET is_favorite = :isFavorite WHERE game_id = :gameId")
    suspend fun updateFavorite(gameId: Long, isFavorite: Boolean)

    @Query("DELETE FROM games WHERE game_id = :gameId")
    suspend fun deleteGame(gameId: Long)

    @Query("DELETE FROM games")
    suspend fun deleteAllGames()

    /** Set all games back to PENDING so they can be re-scraped. */
    @Query("UPDATE games SET scrape_status = 'PENDING'")
    suspend fun resetScrapeStatusToPending()
}
