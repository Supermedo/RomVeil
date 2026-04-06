package com.glyph.launcher.data.remote

import com.glyph.launcher.data.remote.dto.ScreenScraperResponse
import com.glyph.launcher.data.remote.dto.ScreenScraperSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ScreenScraperApi {

    /**
     * Get game info by ROM filename + system ID.
     * The primary lookup is by romnom (ROM filename) + systemeid (system ID).
     * For best results, also pass crc, md5, sha1, romtaille (file size in bytes).
     *
     * API docs: https://www.screenscraper.fr/webapi2.php
     * Base URL: https://api.screenscraper.fr/api2/
     */
    @GET("jeuInfos.php")
    suspend fun getGameInfo(
        @Query("devid") devId: String,
        @Query("devpassword") devPassword: String,
        @Query("softname") softname: String,
        @Query("ssid") ssid: String?,
        @Query("sspassword") ssPassword: String?,
        @Query("output") output: String = "json",
        @Query("romnom") romName: String?,
        @Query("systemeid") systemId: String?,
        @Query("romtaille") romSize: Long? = null,
        @Query("crc") crc: String? = null,
        @Query("md5") md5: String? = null,
        @Query("sha1") sha1: String? = null
    ): ScreenScraperResponse

    /**
     * Search for games by name + system ID.
     * Useful when filename-based lookup fails.
     * Returns a list of matching games.
     */
    @GET("jeuRecherche.php")
    suspend fun searchGame(
        @Query("devid") devId: String,
        @Query("devpassword") devPassword: String,
        @Query("softname") softname: String,
        @Query("ssid") ssid: String?,
        @Query("sspassword") ssPassword: String?,
        @Query("output") output: String = "json",
        @Query("systemeid") systemId: String?,
        @Query("recherche") searchQuery: String
    ): ScreenScraperSearchResponse
}
