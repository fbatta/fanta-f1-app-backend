package net.battaglini.fantaf1appbackend.model

data class RaceWeekendRecap(
    val raceId: String,
    val raceName: String,
    val recapParagraphs: List<String>
)
