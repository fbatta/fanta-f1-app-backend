package net.battaglini.fantaf1appbackend.model.openf1

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.toInstant
import net.battaglini.fantaf1appbackend.deserializer.OpenF1GmtOffsetDeserializer
import net.battaglini.fantaf1appbackend.deserializer.OpenF1TimestampDeserializer
import net.battaglini.fantaf1appbackend.model.RaceWeekend
import tools.jackson.databind.annotation.JsonDeserialize
import kotlin.time.ExperimentalTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenF1MeetingResponse(
    @JsonProperty("circuit_key")
    val circuitKey: Int,
    @JsonProperty("meeting_name")
    val meetingName: String,
    @JsonProperty("meeting_key")
    val meetingKey: Int,
    @JsonProperty("meeting_official_name")
    val meetingOfficialName: String,
    val year: Int,
    @JsonProperty("date_start")
    @JsonDeserialize(using = OpenF1TimestampDeserializer::class)
    val dateStart: LocalDateTime,
    @JsonProperty("date_end")
    @JsonDeserialize(using = OpenF1TimestampDeserializer::class)
    val dateEnd: LocalDateTime,
    @JsonProperty("gmt_offset")
    @JsonDeserialize(using = OpenF1GmtOffsetDeserializer::class)
    val gmtOffset: UtcOffset
) {
    companion object {
        @OptIn(ExperimentalTime::class)
        fun OpenF1MeetingResponse.toRace(
            raceId: String,
            sessions: List<RaceWeekend.Companion.Session> = emptyList()
        ): RaceWeekend = RaceWeekend(
            raceId = raceId,
            openF1MeetingKey = meetingKey,
            raceName = meetingName,
            dateStart = dateStart.toInstant(gmtOffset),
            dateEnd = dateEnd.toInstant(gmtOffset),
            sessions = sessions
        )
    }
}
