package com.owenlejeune.whosinspace.api.model

import com.google.gson.annotations.SerializedName

open class PhotoOfTheDayResponse (
    @SerializedName("copyright") val copyright: String,
    @SerializedName("hdurl") val hdUrl: String,
    @SerializedName("url") val url: String,
    @SerializedName("media_type") val mediaType: PhotoMediaType?
) {
    enum class PhotoMediaType {
        @SerializedName("image")
        IMAGE
    }

    object NonResponse: PhotoOfTheDayResponse("", "", "", null)
}
