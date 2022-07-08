package com.owenlejeune.whosinspace.webcrawler

import com.owenlejeune.whosinspace.api.model.AstronautsResponse
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
            val openNotifyAstronauts = getNamesOfAstronautsInSpace()
            val astronauts = emptyList<Astronaut>().toMutableList()

            awaitAll (
                openNotifyAstronauts.map { astronaut -> launch { astronauts.add(scrapeDataFromWebpage(astronaut, this)) } }
            )

            withContext(Dispatchers.Main) {
                onResponse(astronauts.sortedBy { it.name })
            }
        }
    }

    private suspend fun getNamesOfAstronautsInSpace(): List<AstronautsResponse.Person> {
        val astroResponse = openNotifyService.getAstronautsInSpace()
        if (astroResponse.isSuccessful) {
            val names = astroResponse.body()?.people ?: emptyList<AstronautsResponse.Person>().toMutableList()
            names.forEachIndexed { index, s ->
                if (fixedAstroNames.containsKey(s.name)) {
                    names[index].name = fixedAstroNames[s.name]!!
                }
            }
            return names
        }
        return emptyList()
    }

    private suspend fun scrapeDataFromWebpage(astronaut: AstronautsResponse.Person, scope: CoroutineScope): Astronaut {
        val doc = Jsoup
            .connect("https://www.supercluster.com/astronauts/${astronaut.name.replace(" ", "-").lowercase()}")
            .get()

        val builder = Astronaut.Builder()
        builder.name = astronaut.name
        builder.craft = astronaut.craft

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
        val text = document
            .select("div.px1.py2.container--xl.mxa")
            .select("div.h4")
            .text()
        return text.substring(0, text.length-7)
    }

//    private fun getAstronautName(doc: Document): String {
//        val nameHtml = doc
//            .select("div.astronaut_page__name_desktop.fa.mt025.mr05")
//            .html()
//        return nameHtml.substring(0, nameHtml.indexOf("<div"))
//    }

}