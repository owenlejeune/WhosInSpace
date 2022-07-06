package com.owenlejeune.whosinspace.model

class Astronaut private constructor(
    val name: String,
    val profileImageUrl: String,
    val birthday: String?,
    val flagImageUrl: String,
    val gender: String,
    val occupationOrRank: OccupationOrRank?,
    val numberOfMissions: Int,
    val dayInSpace: Int,
    val profileExcerpt: String
) {
    class OccupationOrRank(
        val type: String,
        val title: String
    )

    class Builder {
        var name = ""
        var profileImageUrl = ""
        var birthday: String? = null
        var flagImageUrl = ""
        var gender = ""
        var occupationOrRank: OccupationOrRank? = null
        var numberOfMissions = 0
        var daysInSpace = 0
        var profileExcerpt = ""

        fun build(): Astronaut {
            return Astronaut(
                name, profileImageUrl, birthday, flagImageUrl, gender,
                occupationOrRank, numberOfMissions, daysInSpace, profileExcerpt
            )
        }
    }

    override fun toString(): String {
        return listOf(
            name, profileImageUrl, birthday, flagImageUrl, gender,
            occupationOrRank, numberOfMissions, dayInSpace, profileImageUrl,
            profileExcerpt
        ).joinToString(separator = "\n") { "$it" }
    }
}