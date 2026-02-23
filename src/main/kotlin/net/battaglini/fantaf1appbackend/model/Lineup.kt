package net.battaglini.fantaf1appbackend.model

import net.battaglini.fantaf1appbackend.deserializer.KotlinInstantDeserializer
import net.battaglini.fantaf1appbackend.serializer.KotlinInstantSerializer
import tools.jackson.databind.annotation.JsonDeserialize
import tools.jackson.databind.annotation.JsonSerialize
import kotlin.time.Instant

data class Lineup(
    val lineupId: String,
    val teamId: String,
    val raceId: String,
    val drivers: List<Driver>,
    @JsonSerialize(using = KotlinInstantSerializer::class)
    @JsonDeserialize(using = KotlinInstantDeserializer::class)
    val createdAt: Instant,
    @JsonSerialize(using = KotlinInstantSerializer::class)
    @JsonDeserialize(using = KotlinInstantDeserializer::class)
    val updatedAt: Instant,
    val version: Int,
    val score: Double?
) {
    companion object {
        data class LineupDriver(
            val driverNumber: Int,
            val driverAcronym: String,
        )
    }
}
