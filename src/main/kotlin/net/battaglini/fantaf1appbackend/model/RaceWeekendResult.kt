package net.battaglini.fantaf1appbackend.model

import net.battaglini.fantaf1appbackend.deserializer.KotlinInstantDeserializer
import net.battaglini.fantaf1appbackend.serializer.KotlinInstantSerializer
import tools.jackson.databind.annotation.JsonDeserialize
import tools.jackson.databind.annotation.JsonSerialize
import kotlin.time.Instant

data class RaceWeekendResult(
    val raceId: String,
    val raceName: String,
    val openF1MeetingKey: Int,
    @JsonSerialize(using = KotlinInstantSerializer::class)
    @JsonDeserialize(using = KotlinInstantDeserializer::class)
    val createdAt: Instant,
    @JsonSerialize(using = KotlinInstantSerializer::class)
    @JsonDeserialize(using = KotlinInstantDeserializer::class)
    val updatedAt: Instant,
    val version: Int,
    val results: List<Result>,
    val summaryParagraphs: List<String>?
) {
    companion object {
        data class Result(
            val driverId: String,
            val driverNumber: Int,
            val driverAcronym: String,
            val points: Double
        )
    }
}
