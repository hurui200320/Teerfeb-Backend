package info.skyblond.teerfeb.backend

import com.google.gson.Gson
import info.skyblond.teerfeb.model.process.CovidRecord
import java.io.File
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.util.*

object CovidDistributionModel {
    val data: Array<CovidRecord>
    val continentToLocationIndex: Map<String, Set<String>>
    val locationToContinentIndex: Map<String, String>
    val sortedDateIndex: SortedSet<LocalDate>

    init {
        data = readData()
        val continentLocationMap = mutableMapOf<String, MutableSet<String>>()
        val locationContinentMap = mutableMapOf<String, String>()
        sortedDateIndex = data.map {
            if (continentLocationMap.containsKey(it.continent.lowercase())) {
                continentLocationMap[it.continent.lowercase()]!!.add(it.location.lowercase())
            } else {
                continentLocationMap[it.continent.lowercase()] = mutableSetOf(it.location.lowercase())
            }

            locationContinentMap[it.location.lowercase()] = it.continent.lowercase()

            it.date
        }.toSortedSet()
        continentToLocationIndex = continentLocationMap
        locationToContinentIndex = locationContinentMap
    }

    private fun readData(): Array<CovidRecord> {
        val gson = Gson()
        return gson.fromJson(
            File("models/covid-distribution.json")
                .readText(StandardCharsets.UTF_8),
            Array<CovidRecord>::class.java
        )
    }
}
