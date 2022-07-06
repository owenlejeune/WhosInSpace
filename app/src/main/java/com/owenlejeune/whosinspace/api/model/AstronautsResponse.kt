package com.owenlejeune.whosinspace.api.model

import com.google.gson.annotations.SerializedName

class AstronautsResponse(
    @SerializedName("number") val number: Int,
    @SerializedName("people") val people: List<Person>
) {

    class Person(
        @SerializedName("name") val name: String,
        @SerializedName("craft") val craft: String
    )

}
