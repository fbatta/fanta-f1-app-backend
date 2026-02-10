package net.battaglini.fantaf1appbackend.model

import net.battaglini.fantaf1appbackend.enums.RaceWeekendSessionType
import net.battaglini.fantaf1appbackend.serializer.KotlinInstantSerializer
import tools.jackson.databind.annotation.JsonSerialize
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
data class RaceWeekend(
    val raceId: String,
    val openF1MeetingKey: Int,
    val raceName: String,
    @JsonSerialize(using = KotlinInstantSerializer::class)
    val dateStart: Instant,
    @JsonSerialize(using = KotlinInstantSerializer::class)
    val dateEnd: Instant,
    val sessions: List<Session>
) {
    fun RaceWeekend.getSessionByType(sessionType: RaceWeekendSessionType): Session? {
        return sessions.firstOrNull { it.sessionType == sessionType }
    }

    companion object {
        data class Session(
            val sessionId: String,
            val openF1SessionKey: Int,
            val sessionType: RaceWeekendSessionType,
            @JsonSerialize(using = KotlinInstantSerializer::class)
            val dateStart: Instant,
            @JsonSerialize(using = KotlinInstantSerializer::class)
            val dateEnd: Instant,
        )
    }
}
