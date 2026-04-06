package com.glyph.launcher.data.remote

import com.glyph.launcher.data.remote.dto.TheGamesDbResponse
import com.glyph.launcher.data.remote.dto.TheGamesDbImagesResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * TheGamesDB API v1 — game metadata and box art.
 * Get an API key at https://api.thegamesdb.net/
 */
interface TheGamesDbApi {

    @GET("v1/Games/ByGameName")
    suspend fun getGamesByName(
        @Query("apikey") apiKey: String,
        @Query("name") name: String,
        @Query("platform_id") platformId: Int? = null
    ): Response<TheGamesDbResponse>

    @GET("v1/Games/Images")
    suspend fun getGameImages(
        @Query("apikey") apiKey: String,
        @Query("games_id") gameId: Int
    ): Response<TheGamesDbImagesResponse>
}
