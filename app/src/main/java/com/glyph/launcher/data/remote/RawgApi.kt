package com.glyph.launcher.data.remote

import com.glyph.launcher.data.remote.dto.RawgResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * RAWG Video Games Database API.
 * Get an API key at https://rawg.io/apidocs
 */
interface RawgApi {

    @GET("games")
    suspend fun searchGames(
        @Query("key") apiKey: String,
        @Query("search") searchTerm: String,
        @Query("platforms") platformIds: String? = null,
        @Query("page_size") pageSize: Int = 5
    ): Response<RawgResponse>
}
