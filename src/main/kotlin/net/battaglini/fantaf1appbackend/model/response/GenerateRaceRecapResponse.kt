package net.battaglini.fantaf1appbackend.model.response

data class GenerateRaceRecapResponse(
    val recapIds: List<String>,
    val recaps: List<RecapEntry>
) {
    data class RecapEntry(
        val raceId: String,
        val raceName: String,
        val recapParagraphs: List<String>
    )
}
