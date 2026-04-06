package com.glyph.launcher.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScreenScraperResponse(
    @SerialName("response")
    val response: ResponseContent? = null
)

@Serializable
data class ResponseContent(
    @SerialName("jeu")
    val game: ScreenScraperGame? = null
)

@Serializable
data class ScreenScraperGame(
    @SerialName("id")
    val id: String? = null,
    @SerialName("noms")
    val noms: List<ScreenScraperName>? = null,
    @SerialName("medias")
    val medias: List<ScreenScraperMedia>? = null,
    @SerialName("synopsis")
    val synopsis: List<ScreenScraperSynopsis>? = null
)

@Serializable
data class ScreenScraperName(
    @SerialName("region")
    val region: String? = null,
    @SerialName("text")
    val text: String? = null
)

@Serializable
data class ScreenScraperMedia(
    @SerialName("type")
    val type: String? = null,
    @SerialName("url")
    val url: String? = null,
    @SerialName("region")
    val region: String? = null
)

@Serializable
data class ScreenScraperSynopsis(
    @SerialName("region")
    val region: String? = null,
    @SerialName("text")
    val text: String? = null
)

// ── Search Response (jeuRecherche.php) ───────────────────────────────────────

@Serializable
data class ScreenScraperSearchResponse(
    @SerialName("response")
    val response: SearchResponseContent? = null
)

@Serializable
data class SearchResponseContent(
    @SerialName("jeux")
    val games: List<ScreenScraperSearchGame>? = null
)

@Serializable
data class ScreenScraperSearchGame(
    @SerialName("id")
    val id: String? = null,
    @SerialName("noms")
    val noms: List<ScreenScraperName>? = null,
    @SerialName("medias")
    val medias: List<ScreenScraperMedia>? = null,
    @SerialName("synopsis")
    val synopsis: List<ScreenScraperSynopsis>? = null,
    @SerialName("systemeid")
    val systemId: String? = null
)
