package com.glyph.launcher.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MobyGamesSearchResponse(
    @SerialName("games")
    val games: List<MobyGamesGame>? = null
)

@Serializable
data class MobyGamesGame(
    @SerialName("game_id")
    val gameId: Int? = null,
    @SerialName("title")
    val title: String? = null,
    @SerialName("platforms")
    val platforms: List<MobyGamesPlatformRef>? = null,
    @SerialName("release_date")
    val releaseDate: String? = null
)

@Serializable
data class MobyGamesPlatformRef(
    @SerialName("platform_id")
    val platformId: Int? = null,
    @SerialName("platform_name")
    val platformName: String? = null
)
