package info.skyblond.teerfeb.model.process

import java.time.LocalDate

data class CovidRecord(
    val date: LocalDate,
    val newCaseCountPerDay: Long,
    val deathCountPerDay: Long,
    val location: String,
    val continent: String
)
