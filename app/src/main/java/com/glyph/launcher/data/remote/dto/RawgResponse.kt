package com.glyph.launcher.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RawgResponse(
    @SerialName("results")
    val results: List<RawgGame>? = null
)

@Serializable
data class RawgGame(
    @SerialName("id")
    val id: Int? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("released")
    val released: String? = null,
    @SerialName("background_image")
    val backgroundImage: String? = null,
    @SerialName("rating")
    val rating: Float? = null
)
