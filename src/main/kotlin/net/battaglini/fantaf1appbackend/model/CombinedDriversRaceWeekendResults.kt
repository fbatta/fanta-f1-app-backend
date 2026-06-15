package net.battaglini.fantaf1appbackend.model

data class CombinedDriversRaceWeekendResults(
    val raceId: String,
    val combinedPracticeResults: List<DriverPracticeResult>,
    val qualifyingResults: List<DriverQualifyingResult>,
    val sprintQualifyingResults: List<DriverQualifyingResult>,
    val raceResults: List<DriverRaceResult>,
    val sprintRaceResults: List<DriverRaceResult>
)
