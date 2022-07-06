package com.owenlejeune.whosinspace.webcrawler

import com.owenlejeune.whosinspace.api.model.OpenNotifyService
import com.owenlejeune.whosinspace.extensions.awaitAll
import com.owenlejeune.whosinspace.model.Astronaut
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class AstroCrawler {

    companion object {
        private val fixedAstroNames = mapOf(
            "Kjell Lindgren" to "Kjell N. Lindgren",
            "Bob Hines" to "Robert Hines"
        )
    }

    private val openNotifyService = OpenNotifyService().createService()

    fun scrapeAstroData(onResponse: (List<Astronaut>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val names = getNamesOfAstronautsInSpace()
            val astronauts = emptyList<Astronaut>().toMutableList()

            awaitAll (
                names.map { name -> launch { astronauts.add(scrapeDataFromWebpage(name, this)) } }
            )

            withContext(Dispatchers.Main) {
                onResponse(astronauts)
            }
        }
    }

    private suspend fun getNamesOfAstronautsInSpace(): List<String> {
        val astroResponse = openNotifyService.getAstronautsInSpace()
        if (astroResponse.isSuccessful) {
            val names = astroResponse.body()?.people?.map { it.name }?.toMutableList() ?: emptyList<String>().toMutableList()
            names.forEachIndexed { index, s ->
                if (fixedAstroNames.containsKey(s)) {
                    names[index] = fixedAstroNames[s]!!
                }
            }
            return names
        }
        return emptyList()
    }

    private suspend fun scrapeDataFromWebpage(name: String, scope: CoroutineScope): Astronaut {
        val doc = Jsoup
            .connect("https://www.supercluster.com/astronauts/${name.replace(" ", "-").lowercase()}")
            .get()

        var profileUrl = ""
        var birthday: String? = null
        var flag = ""
        var gender = ""
        var or = ""
        var numMissions = 0
        var numDays = 0
        var excerpt = ""

        val builder = Astronaut.Builder()

        awaitAll (
            scope.launch { builder.profileImageUrl = getAstronautProfileImage(doc) },
            scope.launch { builder.birthday = getAstronautBirthday(doc) },
            scope.launch { builder.flagImageUrl = getNationalFlag(doc) },
            scope.launch { builder.gender = getGender(doc) },
            scope.launch { builder.occupationOrRank = getOccupationOrRank(doc) },
            scope.launch { builder.numberOfMissions = getNumberOfMissions(doc) },
            scope.launch { builder.daysInSpace= getDaysInSpace(doc) },
            scope.launch { builder.profileExcerpt = getExcerpt(doc) }
        )

        return builder.build()
    }

    private fun getAstronautProfileImage(document: Document): String {
        return document
            .select("div.astronaut_page__image.bcb.f1.rel")
            .first()!!
            .select("div.fill.image__block.abs.x.y.top.left")
            .select("img")
            .attr("src")
    }

    private fun getAstronautBirthday(document: Document): String? {
        val bdayHtml = document
            .select("div.astronaut_page__name_desktop.fa.mt025.mr05")
            .first()!!
            .select("div.x.h4.mt075")
            .html()
        return if (bdayHtml == "Birthdate Unknown") {
            null
        } else {
            bdayHtml.substring(3)
        }
    }

    private fun getNationalFlag(document: Document): String {
        return document.select("div.astronaut_page__flags_tablet.abs.right.pr2.pt05.f.fc")
            .select("div.fill.image__block.abs.x.y.top.left")
            .select("img")
            .attr("src")
    }

    private fun getGender(document: Document): String {
        var gender = ""
        run loop@{
            document.select("div.f.fc.f1").forEach { element ->
                if (element.select("div.mt1.akkura.small.caps").text() == "Gender") {
                    gender = element.select("a").text()
                    return@loop
                }
            }
        }
        return gender
    }

    private fun getOccupationOrRank(document: Document): Astronaut.OccupationOrRank {
        val infoDiv = document.select("div.f.fc.f2").first()!!
        val type = infoDiv.select("div.mt1.akkura.small.caps").text()
        val title = infoDiv.select("div.h4").text()
        return Astronaut.OccupationOrRank(type = type, title = title)
    }

    private fun getNumberOfMissions(document: Document): Int {
        var numMissions = 0
        run loop@ {
            document.select("div.f.fc.f1.jcc").forEach { element ->
                if (element.select("div.akkura.small.caps").not(".mt1").text() == "MISSIONS") {
                    numMissions = element.select("div.astronaut_page__big_stats").text().toInt()
                    return@loop
                }
            }
        }
        return numMissions
    }

    private fun getDaysInSpace(document: Document): Int {
        var numDays = 0
        run loop@ {
            document.select("div.f.fc.f1.jcc").forEach { element ->
                if (element.select("div.mt1.akkura.small.caps").text() == "TIME IN SPACE") {
                    numDays = element.select("span.pr015").not(".pl025").text().toInt()
                    return@loop
                }
            }
        }
        return numDays
    }

    private fun getExcerpt(document: Document): String {
        return document
            .select("div.px1.py2.container--xl.mxa")
            .select("div.h4")
            .text()
    }

//    private fun getAstronautName(doc: Document): String {
//        val nameHtml = doc
//            .select("div.astronaut_page__name_desktop.fa.mt025.mr05")
//            .html()
//        return nameHtml.substring(0, nameHtml.indexOf("<div"))
//    }

}