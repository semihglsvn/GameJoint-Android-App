package com.gamejoint.app.data.model

import com.google.gson.annotations.SerializedName

data class FeaturedGameResponse (
    @SerializedName("gameId")
    val gameId: kotlin.Long? = null,

    @SerializedName("title")
    val title: kotlin.String? = null,

    @SerializedName("customBanner")
    val customBanner: kotlin.String? = null,

    @SerializedName("coverImage")
    val coverImage: kotlin.String? = null,

    // Manually added fields!
    @SerializedName("metascore")
    val metascore: kotlin.Int? = null,

    @SerializedName("genres")
    val genres: kotlin.collections.List<kotlin.String>? = null
)