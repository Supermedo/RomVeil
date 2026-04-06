package com.glyph.launcher.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TheGamesDbResponse(
    @SerialName("data")
    val data: TheGamesDbData? = null
)

@Serializable
data class TheGamesDbData(
    @SerialName("games")
    val games: List<TheGamesDbGame>? = null,
    @SerialName("base_url")
    val baseUrl: TheGamesDbImageBase? = null
)

@Serializable
data class TheGamesDbImageBase(
    @SerialName("original")
    val original: String? = null,
    @SerialName("small")
    val small: String? = null,
    @SerialName("thumb")
    val thumb: String? = null,
    @SerialName("medium")
    val medium: String? = null,
    @SerialName("large")
    val large: String? = null
)

@Serializable
data class TheGamesDbGame(
    @SerialName("id")
    val id: Int? = null,
    @SerialName("game_title")
    val gameTitle: String? = null,
    @SerialName("release_date")
    val releaseDate: String? = null,
    @SerialName("developers")
    val developers: List<Int>? = null,
    @SerialName("genres")
    val genres: List<Int>? = null,
    @SerialName("publishers")
    val publishers: List<Int>? = null
)

@Serializable
data class TheGamesDbImagesResponse(
    @SerialName("data")
    val data: TheGamesDbImagesData? = null
)

@Serializable
data class TheGamesDbImagesData(
    @SerialName("images")
    val images: Map<String, List<TheGamesDbImage>>? = null,
    @SerialName("base_url")
    val baseUrl: TheGamesDbImageBase? = null
)

@Serializable
data class TheGamesDbImage(
    @SerialName("type")
    val type: String? = null,
    @SerialName("side")
    val side: String? = null,
    @SerialName("filename")
    val filename: String? = null
)
