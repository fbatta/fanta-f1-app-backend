package net.battaglini.fantaf1appbackend.model

import net.battaglini.fantaf1appbackend.serializer.KotlinInstantSerializer
import tools.jackson.databind.annotation.JsonSerialize
import kotlin.time.Instant

data class RaceWeekendResult(
    val raceId: String,
    @JsonSerialize(using = KotlinInstantSerializer::class)
    val createdAt: Instant,
    @JsonSerialize(using = KotlinInstantSerializer::class)
    val updatedAt: Instant,
    val version: Int,
    val results: List<Result>
) {
    companion object {
        data class Result(
            val driverId: String,
            val driverNumber: Int,
            val points: Double
        )
    }
}
