package com.glyph.launcher.data.remote

import com.glyph.launcher.data.remote.dto.MobyGamesCoversResponse
import com.glyph.launcher.data.remote.dto.MobyGamesSearchResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * MobyGames API v1.
 * Get an API key at https://www.mobygames.com/info/api/
 */
interface MobyGamesApi {

    @GET("games")
    suspend fun searchGames(
        @Query("api_key") apiKey: String,
        @Query("title") title: String,
        @Query("format") format: String = "normal"
    ): Response<MobyGamesSearchResponse>

    @GET("games/{gameId}/platforms/{platformId}/covers")
    suspend fun getCovers(
        @Path("gameId") gameId: Int,
        @Path("platformId") platformId: Int,
        @Query("api_key") apiKey: String,
        @Query("format") format: String = "normal"
    ): Response<MobyGamesCoversResponse>
}
