package com.glyph.launcher.data.repository

import android.content.Context
import android.util.Log
import android.net.Uri
import com.glyph.launcher.BuildConfig
import com.glyph.launcher.data.local.GameDao
import com.glyph.launcher.data.local.entity.GameEntity
import com.glyph.launcher.data.local.entity.ScrapeStatus
import com.glyph.launcher.data.remote.MobyGamesApi
import com.glyph.launcher.data.remote.RawgApi
import com.glyph.launcher.data.remote.ScreenScraperApi
import com.glyph.launcher.data.remote.TheGamesDbApi
import com.glyph.launcher.data.scanner.RomScanner
import com.glyph.launcher.domain.model.Platform
import com.glyph.launcher.util.HashUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gameDao: GameDao,
    private val theGamesDbApi: TheGamesDbApi,
    private val rawgApi: RawgApi,
    private val mobyGamesApi: MobyGamesApi,
    private val screenScraperApi: ScreenScraperApi,
    private val romScanner: RomScanner,
    private val okHttpClient: OkHttpClient,
    private val preferencesManager: com.glyph.launcher.util.PreferencesManager
) {

    // ── Queries ──────────────────────────────────────────────────────────────

    fun getAllGames(): Flow<List<GameEntity>> = gameDao.getAllGames()

    fun getGamesByPlatform(platformTag: String): Flow<List<GameEntity>> =
        gameDao.getGamesByPlatform(platformTag)

    fun getDistinctPlatforms(): Flow<List<String>> = gameDao.getDistinctPlatforms()

    fun getRecentlyPlayed(): Flow<List<GameEntity>> = gameDao.getRecentlyPlayed()

    fun getFavoriteGames(): Flow<List<GameEntity>> = gameDao.getFavoriteGames()

    suspend fun getGameById(id: Long): GameEntity? = gameDao.getGameById(id)

    suspend fun getGameCount(): Int = gameDao.getGameCount()

    // ── Scanning ─────────────────────────────────────────────────────────────

    suspend fun scanDirectory(
        treeUri: Uri,
        forcePlatform: Platform? = null,
        onProgress: ((Int, String) -> Unit)? = null
    ): RomScanner.ScanResult {
        val existingUris = mutableSetOf<String>()
        val allGames = gameDao.getGamesByScrapeStatus(ScrapeStatus.PENDING) +
                gameDao.getGamesByScrapeStatus(ScrapeStatus.SUCCESS) +
                gameDao.getGamesByScrapeStatus(ScrapeStatus.FAILED) +
                gameDao.getGamesByScrapeStatus(ScrapeStatus.SKIPPED)
        allGames.forEach { existingUris.add(it.fileUri) }

        val result = romScanner.scan(treeUri, forcePlatform, existingUris, onProgress)

        if (result.discovered.isNotEmpty()) {
            gameDao.insertGames(result.discovered)
        }

        return result
    }

    /** Add a single ROM file manually. Returns the inserted game or null if duplicate/unsupported. */
    suspend fun addSingleRom(uri: Uri, platformTag: String): GameEntity? = withContext(Dispatchers.IO) {
        val filename = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0) cursor.getString(nameIndex) else null
        } ?: uri.lastPathSegment?.substringAfterLast("/", "") ?: return@withContext null

        if (gameDao.getGameByUri(uri.toString()) != null) return@withContext null

        val platform = Platform.fromTag(platformTag) ?: return@withContext null

        val displayTitle = HashUtil.cleanFilename(filename).ifBlank { filename }

        val game = GameEntity(
            fileUri = uri.toString(),
            filename = filename,
            displayTitle = displayTitle,
            platformTag = platformTag
        )
        val id = gameDao.insertGame(game)
        if (id > 0) game.copy(gameId = id) else null
    }

    // ── Scraping ─────────────────────────────────────────────────────────────

    /**
     * Scrape metadata + cover art. Tries LibRetro thumbnails first (free, no key needed, actual box art),
     * Scrape metadata + cover art. Tries LibRetro thumbnails first (free, no key needed, actual box art, FAST),
     * then ScreenScraper (best metadata), then falls back to keyed APIs (TheGamesDB, MobyGames, RAWG).
     */
    suspend fun scrapeGame(game: GameEntity): Boolean = withContext(Dispatchers.IO) {
        // Re-fetch from DB so we always use current row (rescrape from UI may pass stale snapshot)
        val currentGame = gameDao.getGameById(game.gameId) ?: return@withContext false
        val platform = Platform.fromTag(currentGame.platformTag) ?: return@withContext false

        // Resolution for MAME/NeoGeo filenames (e.g. "kof94" -> "The King of Fighters '94")
        val mameTitle = if (platform == Platform.NEOGEO || platform == Platform.ARCADE) {
            val real = MameDb.getRealTitle(currentGame.filename)
            Log.d(TAG, "MameDb lookup: '${currentGame.filename}' -> '$real'")
            real
        } else null

        val effectiveGame = if (mameTitle != null) {
            currentGame.copy(filename = "$mameTitle.zip", displayTitle = mameTitle)
        } else {
            currentGame
        }

        val cleanName = HashUtil.cleanFilename(effectiveGame.filename)
        Log.d(TAG, "Scraping: '${effectiveGame.filename}' -> cleaned: '$cleanName' [${platform.tag}]")

        try {
            // ── Step 1: Try LibRetro thumbnails FIRST (free, no API key, FAST) ──
            val libRetroResult = tryLibRetro(effectiveGame, platform)
            if (libRetroResult != null && libRetroResult.imagePath != null) {
                Log.d(TAG, "  libretro -> title='${libRetroResult.title}' ✓ box art found")
                val fixedTitle = normalizeScrapedTitle(libRetroResult.title)
                gameDao.updateScrapedData(
                    gameId = currentGame.gameId,
                    displayTitle = fixedTitle,
                    releaseDate = libRetroResult.year,
                    developer = null,
                    genre = null,
                    backgroundImagePath = libRetroResult.imagePath,
                    scrapeStatus = ScrapeStatus.SUCCESS,
                    rating = null
                )
                return@withContext true
            }

            // ── Step 2: Try ScreenScraper (single jeuInfos call, skip search for speed) ──
            val ssResult = tryScreenScraper(effectiveGame, platform)
            if (ssResult != null && ssResult.imagePath != null) {
                Log.d(TAG, "  screenscraper -> title='${ssResult.title}' ✓ cover found")
                val fixedTitle = normalizeScrapedTitle(ssResult.title)
                gameDao.updateScrapedData(
                    gameId = currentGame.gameId,
                    displayTitle = fixedTitle,
                    releaseDate = ssResult.year,
                    developer = null,
                    genre = ssResult.description,
                    backgroundImagePath = ssResult.imagePath,
                    scrapeStatus = ScrapeStatus.SUCCESS,
                    rating = null
                )
                return@withContext true
            } else if (ssResult != null) {
                // Got title/desc but no image — save title, try other sources for image
                val fixedTitle = normalizeScrapedTitle(ssResult.title)
                gameDao.updateScrapedData(
                    gameId = currentGame.gameId,
                    displayTitle = fixedTitle,
                    releaseDate = ssResult.year,
                    developer = null,
                    genre = ssResult.description,
                    backgroundImagePath = null,
                    scrapeStatus = ScrapeStatus.PENDING,
                    rating = null
                )
            }

            // ── Step 3: Fall back to keyed APIs ──
            val tgdbKey = preferencesManager.theGamesDbApiKey.first().ifBlank { BuildConfig.THEGAMESDB_API_KEY }
            val rawgKey = preferencesManager.rawgApiKey.first().ifBlank { BuildConfig.RAWG_API_KEY }
            val mobyKey = preferencesManager.mobyGamesApiKey.first().ifBlank { BuildConfig.MOBYGAMES_API_KEY }

            val scrapersWithKeys = SCRAPER_IDS.filter { id ->
                when (id) {
                    "thegamesdb" -> tgdbKey.isNotBlank()
                    "rawg" -> rawgKey.isNotBlank()
                    "mobygames" -> mobyKey.isNotBlank()
                    else -> false
                }
            }
            val defaultScraper = preferencesManager.defaultScraper.first()
            val scrapers = if (defaultScraper != "default" && defaultScraper in scrapersWithKeys) {
                listOf(defaultScraper) + scrapersWithKeys.filter { it != defaultScraper }
            } else {
                listOf("thegamesdb", "mobygames", "rawg").filter { it in scrapersWithKeys }
            }

            var bestResult: ScrapeResult? = null
            var bestSource: String? = null

            for (scraperId in scrapers) {
                val result = when (scraperId) {
                    "thegamesdb" -> tryTheGamesDb(effectiveGame, platform, tgdbKey)
                    "rawg" -> tryRawg(effectiveGame, platform, rawgKey)
                    "mobygames" -> tryMobyGames(effectiveGame, platform, mobyKey)
                    else -> null
                }
                if (result != null) {
                    val isBetterSource = bestResult == null ||
                        (result.imagePath != null && bestResult?.imagePath == null) ||
                        (result.imagePath != null && bestSource == "rawg" && scraperId != "rawg")
                    if (isBetterSource) {
                        bestResult = result
                        bestSource = scraperId
                    }
                    if (result.imagePath != null && scraperId != "rawg") break
                }
            }

            if (bestResult == null) {
                Log.d(TAG, "  No results from any scraper for '${effectiveGame.filename}'")
                // Mark as FAILED so the user sees it wasn't scraped
                gameDao.updateScrapedData(
                    gameId = currentGame.gameId,
                    displayTitle = currentGame.displayTitle,
                    releaseDate = currentGame.releaseDate,
                    developer = currentGame.developer,
                    genre = currentGame.genre,
                    backgroundImagePath = currentGame.backgroundImagePath,
                    scrapeStatus = ScrapeStatus.FAILED,
                    rating = currentGame.rating
                )
                return@withContext false
            }

            val result = bestResult!!
            val pathToUse = result.imagePath ?: currentGame.backgroundImagePath
            val fixedTitle = normalizeScrapedTitle(result.title)
            gameDao.updateScrapedData(
                gameId = currentGame.gameId,
                displayTitle = fixedTitle,
                releaseDate = result.year,
                developer = null,
                genre = null,
                backgroundImagePath = pathToUse,
                scrapeStatus = ScrapeStatus.SUCCESS,
                rating = result.rating
            )
            return@withContext true
        } catch (e: Exception) {
            // On error, preserve existing data (do not clear cover)
            gameDao.updateScrapedData(
                gameId = currentGame.gameId,
                displayTitle = currentGame.displayTitle,
                releaseDate = currentGame.releaseDate,
                developer = currentGame.developer,
                genre = currentGame.genre,
                backgroundImagePath = currentGame.backgroundImagePath,
                scrapeStatus = ScrapeStatus.FAILED,
                rating = currentGame.rating
            )
            return@withContext false
        }
    }

    suspend fun resetAllScrapeStatusToPending() = withContext(Dispatchers.IO) {
        gameDao.resetScrapeStatusToPending()
    }

    suspend fun scrapeAllPending(
        onProgress: ((current: Int, total: Int, title: String) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        // Pick up both PENDING and FAILED games so failed ones get retried
        val pending = gameDao.getGamesByScrapeStatus(ScrapeStatus.PENDING) +
                      gameDao.getGamesByScrapeStatus(ScrapeStatus.FAILED)
        if (pending.isEmpty()) return@withContext
        onProgress?.invoke(0, pending.size, "")
        val done = java.util.concurrent.atomic.AtomicInteger(0)
        for (chunk in pending.chunked(SCRAPE_CONCURRENCY)) {
            coroutineScope {
                chunk.map { game ->
                    async {
                        scrapeGame(game)
                        val current = done.incrementAndGet()
                        onProgress?.invoke(current, pending.size, game.displayTitle)
                    }
                }.awaitAll()
            }
            delay(SCRAPE_DELAY_MS)
        }
    }

    // ── Play Tracking ────────────────────────────────────────────────────────

    suspend fun recordPlay(gameId: Long) {
        gameDao.recordPlay(gameId)
    }

    suspend fun setPreferredEmulator(gameId: Long, packageName: String) {
        gameDao.setPreferredEmulator(gameId, packageName)
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    /** LibRetro thumbails system name (used for thumbnails.libretro.com). */
    private fun libRetroSystemName(platform: Platform): String? = when (platform) {
        Platform.NES -> "Nintendo - Nintendo Entertainment System"
        Platform.SNES -> "Nintendo - Super Nintendo Entertainment System"
        Platform.N64 -> "Nintendo - Nintendo 64"
        Platform.GB -> "Nintendo - Game Boy"
        Platform.GBC -> "Nintendo - Game Boy Color"
        Platform.GBA -> "Nintendo - Game Boy Advance"
        Platform.NDS -> "Nintendo - Nintendo DS"
        Platform.GENESIS -> "Sega - Mega Drive - Genesis"
        Platform.SEGA32X -> "Sega - 32X"
        Platform.SATURN -> "Sega - Saturn"
        Platform.DREAMCAST -> "Sega - Dreamcast"
        Platform.PSX -> "Sony - PlayStation"
        Platform.PS2 -> "Sony - PlayStation 2"
        Platform.PSP -> "Sony - PlayStation Portable"
        Platform.GAMECUBE -> "Nintendo - GameCube"
        Platform.WII -> "Nintendo - Wii"
        Platform.ARCADE -> "MAME"
        Platform.NEOGEO -> "SNK - Neo Geo"
        else -> null
    }

    /**
     * Try to find box art from LibRetro thumbnails (free, no API key).
     * URL: https://thumbnails.libretro.com/{System}/Named_Boxarts/{Game}.png
     * Special chars & * / : < > ? \\ | " in game name are replaced with _
     */
    private suspend fun tryLibRetro(
        game: GameEntity,
        platform: Platform
    ): ScrapeResult? = withContext(Dispatchers.IO) {
        val systemName = libRetroSystemName(platform) ?: return@withContext null
        val baseName = game.filename.substringBeforeLast(".")

        // Helper to remove brackets/parens but keep punctuation
        fun stripTags(s: String): String = s
            .replace(Regex("\\(.*?\\)"), "")
            .replace(Regex("\\[.*?]") , "")
            .replace(Regex("\\s+"), " ")
            .trim()

        val candidates = mutableListOf<String>()
        val strict = baseName
        val minimal = stripTags(baseName)
        
        // Priority 0: User-defined title
        candidates.add(game.displayTitle)

        // Priority 1: Raw filename (minus extension)
        candidates.add(baseName)

        candidates.add(minimal)
        if (strict != minimal) candidates.add(strict)

        // "The Legend of Zelda" -> "Legend of Zelda, The"
        if (minimal.startsWith("The ", ignoreCase = true)) {
            candidates.add(minimal.substring(4).trim() + ", The")
        }
        
        // Replace '&' with 'and'
        if (minimal.contains("&")) {
            candidates.add(minimal.replace("&", "and"))
        }

        // Replace ' - ' with ': '
        if (minimal.contains(" - ")) {
            candidates.add(minimal.replace(" - ", ": "))
        }

        // --- REGION FIX ---
        val regionMap = mapOf(
            "(U)" to "(USA)",
            "(E)" to "(Europe)",
            "(J)" to "(Japan)"
        )
        
        regionMap.forEach { (code, fullRegion) ->
            if (game.filename.contains(code, ignoreCase = true)) {
                candidates.add("$minimal $fullRegion")
                if (strict != minimal) {
                    candidates.add("$strict $fullRegion")
                }
            }
        }

        val uniqueCandidates = candidates.distinct()

        for (gameName in uniqueCandidates) {
            val encodedSystem = java.net.URLEncoder.encode(systemName, "UTF-8").replace("+", "%20")
            
            val strategies = mutableListOf<String>()
            
            // Strategy A: Replace special chars with _
            strategies.add(gameName.replace(Regex("[&*/:<>?\\\\|\"']"), "_"))
            
            // Strategy B: Keep original name (URL-encoded)
            if (gameName != strategies[0]) strategies.add(gameName)

            for (sanitized in strategies) {
                val encodedName = java.net.URLEncoder.encode(sanitized, "UTF-8").replace("+", "%20")
                val url = "https://thumbnails.libretro.com/$encodedSystem/Named_Boxarts/$encodedName.png"

                try {
                    val request = okhttp3.Request.Builder().url(url).head().build()
                    val response = okHttpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        val imagePath = downloadImage(url, game.gameId)
                        if (imagePath != null) {
                            return@withContext ScrapeResult(
                                title = gameName,
                                year = null,
                                imagePath = imagePath,
                                rating = null
                            )
                        }
                    }
                    response.close()
                } catch (_: Exception) { /* next */ }
            }
        }
        null
    }

    // ── Keyed Scrapers ──────────────────────────────────────────────────────

    private fun theGamesDbPlatformId(platform: Platform): Int? = when (platform) {
        Platform.NES -> 7
        Platform.SNES -> 8
        Platform.N64 -> 9
        Platform.GB -> 10
        Platform.GBC -> 11
        Platform.GBA -> 12
        Platform.NDS -> 13
        Platform.GENESIS -> 14
        Platform.PSX -> 15
        Platform.PS2 -> 16
        Platform.PSP -> 17
        Platform.DREAMCAST -> 18
        Platform.SATURN -> 19
        Platform.GAMECUBE -> 21
        Platform.WII -> 38
        Platform.ARCADE -> 23
        Platform.NEOGEO -> 66
        Platform.SEGA32X -> 29
        else -> null
    }

    /** RAWG API platform IDs (api.rawg.io/api/platforms). */
    private fun rawgPlatformId(platform: Platform): Int? = when (platform) {
        Platform.NES -> 18
        Platform.SNES -> 16
        Platform.N64 -> 21
        Platform.GB -> 15
        Platform.GBC -> 17
        Platform.GBA -> 19
        Platform.NDS -> 20
        Platform.GENESIS -> 23
        Platform.SEGA32X -> 23
        Platform.SATURN -> 74
        Platform.DREAMCAST -> 22
        Platform.PSX -> 7
        Platform.PS2 -> 8
        Platform.PSP -> 14
        Platform.GAMECUBE -> 83
        Platform.WII -> 20
        Platform.ARCADE -> 52
        Platform.NEOGEO -> 136
        else -> null
    }

    private suspend fun tryTheGamesDb(
        game: GameEntity,
        platform: Platform,
        apiKey: String
    ): ScrapeResult? = withContext(Dispatchers.IO) {
        try {
            val cleanName = HashUtil.cleanFilename(game.filename)
            val generatedShort = cleanName.split(Regex("\\s+")).filter { it.length > 1 }.take(3).joinToString(" ").ifBlank { cleanName }
            val tgdbPlatformId = theGamesDbPlatformId(platform)

            // Search precedence: User Title -> Clean Filename -> Shortened Filename
            val searchTerms = listOf(game.displayTitle, cleanName, generatedShort).distinct()

            // Only search with platform filter — never search globally
            for (name in searchTerms) {
                val response = theGamesDbApi.getGamesByName(
                    apiKey = apiKey,
                    name = name,
                    platformId = tgdbPlatformId
                )
                val body = response.body()?.data ?: continue
                val games = body.games ?: continue
                
                // Log candidates
                games.forEach { g ->
                    val score = matchScore(cleanName, g.gameTitle ?: "")
                    Log.e("GlyphScraper", "  TGDB candidate: '${g.gameTitle}' score=$score (required > 0.4)")
                }

                val best = games
                    .map { g -> Triple(g, matchScore(cleanName, g.gameTitle ?: ""), leadingMatchWords(cleanName, g.gameTitle ?: "")) }
                    .filter { it.second >= 0.40f }
                    .maxWithOrNull(compareBy<Triple<*, Float, Int>> { it.second }.thenByDescending { it.third })
                    ?: continue
                val (first, score, _) = best
                Log.d(TAG, "    TGDB match: '${first.gameTitle}' score=$score")
                val rawTitle = first.gameTitle ?: game.displayTitle
                val year = first.releaseDate?.take(4)
                val gameId = first.id ?: continue

                val imgResponse = theGamesDbApi.getGameImages(apiKey = apiKey, gameId = gameId)
                val imgData = imgResponse.body()?.data ?: continue
                val baseUrl = imgData.baseUrl?.original ?: imgData.baseUrl?.large
                    ?: imgData.baseUrl?.medium ?: continue
                val images = imgData.images?.get(gameId.toString()) ?: emptyList()
                // Prefer FRONT boxart, then any boxart, then fanart
                val imageChoice = images.find { it.type?.lowercase() == "boxart" && it.side?.lowercase() == "front" }
                    ?: images.find { it.type?.lowercase() == "boxart" }
                    ?: images.find { it.type?.lowercase() == "fanart" }
                    ?: images.firstOrNull()
                val imagePath = imageChoice?.filename?.let { filename ->
                    downloadImage(baseUrl.trimEnd('/') + "/" + filename, game.gameId)
                }
                return@withContext ScrapeResult(rawTitle, year, imagePath, null)
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "TGDB error: ${e.message}")
            null
        }
    }

    private suspend fun tryRawg(
        game: GameEntity,
        platform: Platform,
        apiKey: String
    ): ScrapeResult? = withContext(Dispatchers.IO) {
        try {
            val cleanName = HashUtil.cleanFilename(game.filename)
            val platformId = rawgPlatformId(platform)

            val generatedShort = cleanName.split(Regex("\\s+")).filter { it.length > 1 }.take(3).joinToString(" ").ifBlank { cleanName }
            
            // Search precedence: User Title -> Clean Filename -> Shortened
            val searchTerms = listOf(game.displayTitle, cleanName, generatedShort).distinct()

            // Only search with platform filter — never search globally
            for (term in searchTerms) {
                val first = rawgSearchAndPick(cleanName, term, platformId?.toString(), apiKey) ?: continue
                val rawTitle = first.name ?: game.displayTitle
                // Accept rating if valid
                val rating = first.rating?.coerceIn(0f, 5f)
                
                val year = first.released?.take(4)
                // Note: RAWG background_image is a screenshot, not box art.
                // We only use it if score is VERY high (implying definitive match)
                val imagePath = first.backgroundImage?.let { downloadImage(it, game.gameId) }
                
                Log.d(TAG, "    RAWG match: '${first.name}'")
                return@withContext ScrapeResult(rawTitle, year, imagePath, rating)
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "RAWG error: ${e.message}")
            null
        }
    }

    private suspend fun rawgSearchAndPick(
        cleanName: String,
        searchTerm: String,
        platformIds: String?,
        apiKey: String
    ): com.glyph.launcher.data.remote.dto.RawgGame? {
        val response = rawgApi.searchGames(
            apiKey = apiKey,
            searchTerm = searchTerm,
            platformIds = platformIds,
            pageSize = 10
        )
        val results = response.body()?.results ?: return null
        
        // Log all candidates for debugging
        results.forEach { r ->
             val score = matchScore(cleanName, r.name ?: "")
             Log.e("GlyphScraper", "  RAWG candidate: '${r.name}' score=$score (required > 0.85)")
        }

        return results
            .map { r -> Triple(r, matchScore(cleanName, r.name ?: ""), leadingMatchWords(cleanName, r.name ?: "")) }
            // CRITICALLY RAISED THRESHOLD: RAWG returns screenshots, so only accept if we are VERY sure it's the right game.
            // 0.40 let in "Asteroids Hyper" -> "Asteroids Hyper" (screenshot) but also "BattleTanx" -> "BattleTanks" (wrong game).
            // 0.85 was too strict (rejected Carmageddon 64 -> Carmageddon). 0.70 is a balanced compromise.
            .filter { it.second >= 0.70f }
            .maxWithOrNull(compareBy<Triple<*, Float, Int>> { it.second }.thenByDescending { it.third })
            ?.first
    }

    private suspend fun tryMobyGames(
        game: GameEntity,
        platform: Platform,
        apiKey: String
    ): ScrapeResult? = withContext(Dispatchers.IO) {
        try {
            val cleanName = HashUtil.cleanFilename(game.filename)
            val shortened = cleanName.split(Regex("\\s+")).filter { it.length > 1 }.take(3).joinToString(" ").ifBlank { cleanName }
            val searchTerms = listOf(game.displayTitle, cleanName, shortened).distinct()
            
            for (searchTitle in searchTerms) {
                val response = mobyGamesApi.searchGames(
                    apiKey = apiKey,
                    title = searchTitle,
                    format = "normal"
                )
                val games = response.body()?.games ?: continue
                val first = games
                    .map { g -> Triple(g, matchScore(cleanName, g.title ?: ""), leadingMatchWords(cleanName, g.title ?: "")) }
                    .filter { it.second >= 0.75f }
                    .maxWithOrNull(compareBy<Triple<*, Float, Int>> { it.second }.thenByDescending { it.third })
                    ?.first ?: continue
                val rawTitle = first.title ?: game.displayTitle
                val year = first.releaseDate?.take(4)
                val gameId = first.gameId ?: continue
                val platformRef = first.platforms?.firstOrNull()
                val platformId = platformRef?.platformId ?: continue

                val coversResponse = mobyGamesApi.getCovers(
                    gameId = gameId,
                    platformId = platformId,
                    apiKey = apiKey,
                    format = "normal"
                )
                val coverGroups = coversResponse.body()?.coverGroups ?: continue
                val coverUrl = coverGroups.firstOrNull()?.covers?.firstOrNull()?.image
                    ?: coverGroups.firstOrNull()?.covers?.firstOrNull()?.thumbnailImage
                val imagePath = coverUrl?.let { downloadImage(it, game.gameId) }
                return@withContext ScrapeResult(rawTitle, year, imagePath, null)
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun downloadImage(url: String, gameId: Long): String? =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                val response = okHttpClient.newCall(request).execute()

                if (!response.isSuccessful) return@withContext null

                val imagesDir = File(context.filesDir, "backgrounds")
                if (!imagesDir.exists()) imagesDir.mkdirs()

                val extension = url.substringAfterLast(".", "jpg")
                    .substringBefore("?")
                    .take(4)
                // Unique filename per download so rescrape updates DB path and UI shows new cover
                val file = File(imagesDir, "bg_${gameId}_${System.currentTimeMillis()}.$extension")

                response.body?.byteStream()?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                // Remove old cover files for this game (so we don't accumulate; old path = same cover in UI)
                imagesDir.listFiles()?.filter { it.name.startsWith("bg_${gameId}") && it.name != file.name }?.forEach { it.delete() }

                file.absolutePath
            } catch (e: Exception) {
                null
            }
        }

    suspend fun deleteAllGames() {
        val imagesDir = File(context.filesDir, "backgrounds")
        if (imagesDir.exists()) imagesDir.deleteRecursively()
        gameDao.deleteAllGames()
    }

    suspend fun deleteGame(gameId: Long) {
        val game = gameDao.getGameById(gameId) ?: return
        val path = game.backgroundImagePath
        if (path != null) {
            try {
                File(path).delete()
            } catch (_: Exception) { }
        }
        gameDao.deleteGame(gameId)
    }

    suspend fun updateGameCoverPath(gameId: Long, imagePath: String?) {
        gameDao.updateBackgroundImage(gameId, imagePath)
    }

    suspend fun updateGameTitle(gameId: Long, title: String) {
        gameDao.updateGameTitle(gameId, title)
    }

    suspend fun updateFavorite(gameId: Long, isFavorite: Boolean) {
        gameDao.updateFavorite(gameId, isFavorite)
    }

    private data class ScrapeResult(
        val title: String,
        val year: String?,
        val imagePath: String?,
        val rating: Float?,
        val description: String? = null
    )

    /** Normalize scraped title before saving: trim, collapse spaces, trim punctuation so names display clean. */
    private fun normalizeScrapedTitle(title: String): String {
        if (title.isBlank()) return title
        return title
            .replace(Regex("\\s+"), " ")
            .trim()
            .trimEnd('.', ',', ':', ';', '-', '–', '—')
            .trim()
            .ifBlank { title.trim() }
    }

    /** Common words to skip when requiring a "main" title word (avoids "The Legend of Zelda" failing on "the"). */
    private val STOP_WORDS = setOf("the", "a", "an", "of", "and", "for", "in", "to")

    /**
     * Score how well scraped title matches the ROM filename (0f = no match, 1f = perfect).
     * Uses a combination of token overlap and Levenshtein distance.
     */
    private fun matchScore(cleanName: String, scrapedTitle: String): Float {
        val c = cleanName.lowercase().trim()
        val s = scrapedTitle.lowercase().trim()
        if (c.isBlank() || s.isBlank()) return 0f
        if (c == s) return 1.0f
        
        // If clean name is very short (e.g. "kof98"), strict containment check
        if (c.length <= 4) {
             return if (c == s || s == c) 1.0f else 0f
        }

        // Token-based Jaccard similarity
        val cWords = c.split(Regex("\\s+")).filter { it.length > 1 }
        val sWords = s.split(Regex("\\s+")).filter { it.length > 1 }
        
        if (cWords.isEmpty() || sWords.isEmpty()) return 0f

        val intersect = cWords.intersect(sWords.toSet()).size
        val union = (cWords.toSet() + sWords.toSet()).size
        val jaccard = if (union > 0) intersect.toFloat() / union else 0f

        // Levenshtein similarity for more nuance
        val levenshtein = calculateLevenshteinSimilarity(c, s)

        // Weighted score: 40% Jaccard (exact word matches), 60% Levenshtein (overall structure)
        return (jaccard * 0.4f) + (levenshtein * 0.6f)
    }

    private fun calculateLevenshteinSimilarity(s1: String, s2: String): Float {
        val dist = levenshteinDistance(s1, s2)
        val maxLen = maxOf(s1.length, s2.length)
        if (maxLen == 0) return 1.0f
        return 1.0f - (dist.toFloat() / maxLen)
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val lhsLength = s1.length
        val rhsLength = s2.length
        var cost = IntArray(lhsLength + 1) { it }
        var newCost = IntArray(lhsLength + 1)

        for (i in 1..rhsLength) {
            newCost[0] = i
            for (j in 1..lhsLength) {
                val match = if (s1[j - 1] == s2[i - 1]) 0 else 1
                val costReplace = cost[j - 1] + match
                val costInsert = cost[j] + 1
                val costDelete = newCost[j - 1] + 1
                newCost[j] = minOf(costInsert, costDelete, costReplace)
            }
            val swap = cost
            cost = newCost
            newCost = swap
        }
        return cost[lhsLength]
    }

    /** Tie-breaker: prefer result where scraped title starts with same words as ROM. */
    private fun leadingMatchWords(cleanName: String, scrapedTitle: String): Int {
        if (scrapedTitle.isBlank() || cleanName.isBlank()) return 0
        val want = cleanName.lowercase().trim().split(Regex("\\s+")).filter { it !in STOP_WORDS }
        val have = scrapedTitle.lowercase().split(Regex("\\s+")).map { it.trimEnd('.', ',', ':') }
        var i = 0
        while (i < want.size && i < have.size && have[i] == want[i]) i++
        return i
    }

    // ── ScreenScraper ────────────────────────────────────────────────────────

    /** If SS returns 403, stop trying for the rest of this session (bad credentials). */
    @Volatile private var ssDisabledThisSession = false

    private suspend fun tryScreenScraper(game: GameEntity, platform: Platform): ScrapeResult? {
        if (ssDisabledThisSession) return null

        val devId = com.glyph.launcher.BuildConfig.SCREEN_SCRAPER_DEV_ID
        val devPass = com.glyph.launcher.BuildConfig.SCREEN_SCRAPER_DEV_PASSWORD
        val user = preferencesManager.ssUser.first()
        val pass = preferencesManager.ssPass.first()

        val systemId = platform.screenScraperId
        val romName = game.filename

        try {
            val response = screenScraperApi.getGameInfo(
                devId = devId,
                devPassword = devPass,
                softname = "GlyphLauncher",
                ssid = user,
                ssPassword = pass,
                romName = romName,
                systemId = systemId.toString()
            )

            val gameInfo = response.response?.game
            if (gameInfo != null) {
                Log.d(TAG, "  SS: found game id=${gameInfo.id} for '$romName'")
                return extractScrapeResult(gameInfo, game)
            }
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (msg.contains("403")) {
                Log.w(TAG, "  SS: 403 Forbidden — bad credentials, disabling SS for this session")
                ssDisabledThisSession = true
                return null
            }
            if (isRateLimited(e)) {
                kotlinx.coroutines.delay(2000)
            }
        }

        return null
    }

    private fun isRateLimited(e: Exception): Boolean {
        val msg = e.message ?: return false
        return msg.contains("429") || msg.contains("430") || msg.contains("Too Many")
    }

    /** Extract ScrapeResult from jeuInfos response game object */
    private suspend fun extractScrapeResult(
        gameInfo: com.glyph.launcher.data.remote.dto.ScreenScraperGame,
        game: GameEntity
    ): ScrapeResult? {
        val name = gameInfo.noms?.find { it.region?.lowercase() == "ss" }?.text
            ?: gameInfo.noms?.find { it.region?.lowercase() == "us" }?.text
            ?: gameInfo.noms?.find { it.region?.lowercase() == "en" }?.text
            ?: gameInfo.noms?.firstOrNull()?.text
            ?: game.displayTitle

        val desc = gameInfo.synopsis?.find { it.region?.lowercase() == "en" }?.text
            ?: gameInfo.synopsis?.firstOrNull()?.text

        var mediaUrl = gameInfo.medias?.find { it.type == "box-2D" }?.url
            ?: gameInfo.medias?.find { it.type == "box-2d" }?.url
            ?: gameInfo.medias?.find { it.type == "box-3D" }?.url
            ?: gameInfo.medias?.find { it.type == "box-3d" }?.url
            ?: gameInfo.medias?.find { it.type == "screenmarquee" }?.url
            ?: gameInfo.medias?.find { it.type == "wheel" }?.url
            ?: gameInfo.medias?.find { it.type == "ss" }?.url
            ?: gameInfo.medias?.find { it.type == "sstitle" }?.url

        Log.d(TAG, "  SS: name='$name', mediaUrl=$mediaUrl, medias=${gameInfo.medias?.map { it.type }}")

        val imagePath = if (mediaUrl != null) downloadImage(mediaUrl, game.gameId) else null

        return ScrapeResult(
            title = name ?: game.displayTitle,
            year = null,
            imagePath = imagePath,
            rating = null,
            description = desc
        )
    }

    /** Extract ScrapeResult from jeuRecherche search result */
    private suspend fun extractSearchScrapeResult(
        searchGame: com.glyph.launcher.data.remote.dto.ScreenScraperSearchGame,
        game: GameEntity
    ): ScrapeResult? {
        val name = searchGame.noms?.find { it.region?.lowercase() == "ss" }?.text
            ?: searchGame.noms?.find { it.region?.lowercase() == "us" }?.text
            ?: searchGame.noms?.find { it.region?.lowercase() == "en" }?.text
            ?: searchGame.noms?.firstOrNull()?.text
            ?: game.displayTitle

        val desc = searchGame.synopsis?.find { it.region?.lowercase() == "en" }?.text
            ?: searchGame.synopsis?.firstOrNull()?.text

        var mediaUrl = searchGame.medias?.find { it.type == "box-2D" }?.url
            ?: searchGame.medias?.find { it.type == "box-2d" }?.url
            ?: searchGame.medias?.find { it.type == "box-3D" }?.url
            ?: searchGame.medias?.find { it.type == "box-3d" }?.url
            ?: searchGame.medias?.find { it.type == "wheel" }?.url
            ?: searchGame.medias?.find { it.type == "ss" }?.url

        val imagePath = if (mediaUrl != null) downloadImage(mediaUrl, game.gameId) else null

        return ScrapeResult(
            title = name ?: game.displayTitle,
            year = null,
            imagePath = imagePath,
            rating = null,
            description = desc
        )
    }

    // System IDs are now sourced directly from Platform.screenScraperId

    companion object {
        private const val TAG = "GlyphScraper"
        private val SCRAPER_IDS = listOf("thegamesdb", "rawg", "mobygames")
        /** Max games scraped in parallel. LibRetro has no rate limits. */
        private const val SCRAPE_CONCURRENCY = 6
        /** Delay (ms) between batches. */
        private const val SCRAPE_DELAY_MS = 100L
    }
}
