package com.glyph.launcher.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MobyGamesCoversResponse(
    @SerialName("cover_groups")
    val coverGroups: List<MobyGamesCoverGroup>? = null
)

@Serializable
data class MobyGamesCoverGroup(
    @SerialName("covers")
    val covers: List<MobyGamesCover>? = null
)

@Serializable
data class MobyGamesCover(
    @SerialName("image")
    val image: String? = null,
    @SerialName("thumbnail_image")
    val thumbnailImage: String? = null
)
