package info.skyblond.teerfeb.model.process

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.opencsv.CSVReader
import java.io.File
import java.nio.charset.StandardCharsets
import java.time.LocalDate

/**
 * https://www.ecdc.europa.eu/en/publications-data/download-todays-data-geographic-distribution-covid-19-cases-worldwide
 * */
object TimeProcess {

    private val rawCSVFile = File("dataset/COVID-19-geographic-disbtribution-worldwide.csv")
    private val processedJsonFile = File("models/covid-distribution.json")

    @JvmStatic
    fun main(args: Array<String>) {
        val lines = rawCSVFile.reader(StandardCharsets.UTF_8)
            .use { reader ->
                CSVReader(reader).use {
                    it.readAll()
                }
            }
        val headers = lines[0]
//        headers.forEachIndexed { index, s -> println("$index, // $s") }

        val entities = lines.drop(1)
            .map { line ->
                mutableMapOf<String, String>()
                    .also {
                        line.forEachIndexed { index, s ->
                            it[headers[index]] = s
                        }
                    }
            }
            .map {
                CovidRecord(
                    date = LocalDate.of(
                        it["year"]!!.toInt(),
                        it["month"]!!.toInt(),
                        it["day"]!!.toInt()
                    ),
                    newCaseCountPerDay = it["cases"]!!.toLong(),
                    deathCountPerDay = it["deaths"]!!.toLong(),
                    location = it["countriesAndTerritories"]!!,
                    continent = it["continentExp"]!!,
                )
            }

        val gson = GsonBuilder().setPrettyPrinting().create()
        processedJsonFile.writeText(
            gson.toJson(entities),
            StandardCharsets.UTF_8
        )
    }
}
