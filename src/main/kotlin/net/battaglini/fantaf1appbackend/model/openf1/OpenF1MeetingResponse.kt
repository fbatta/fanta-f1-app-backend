package net.battaglini.fantaf1appbackend.model.openf1

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.datetime.*
import net.battaglini.fantaf1appbackend.deserializer.OpenF1GmtOffsetDeserializer
import net.battaglini.fantaf1appbackend.deserializer.OpenF1TimestampDeserializer
import net.battaglini.fantaf1appbackend.model.RaceWeekend
import tools.jackson.databind.annotation.JsonDeserialize
import kotlin.time.ExperimentalTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenF1MeetingResponse(
    @JsonProperty("circuit_key")
    val circuitKey: Int,
    @JsonProperty("circuit_image")
    val circuitImage: String,
    @JsonProperty("meeting_name")
    val meetingName: String,
    @JsonProperty("meeting_key")
    val meetingKey: Int,
    @JsonProperty("meeting_official_name")
    val meetingOfficialName: String,
    @JsonProperty("country_name")
    val countryName: String,
    @JsonProperty("country_flag")
    val countryFlag: String,
    @JsonProperty("circuit_type")
    val circuitType: String,
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
        ): RaceWeekend {
            val instantStart = dateStart.toInstant(gmtOffset)
            val instantEnd = dateEnd.toInstant(gmtOffset)

            val dateLineupOpen = instantStart.minus(3 * 24, DateTimeUnit.HOUR)
            val dateLineupClose =
                LocalDateTime(dateStart.year, dateStart.month, dateStart.day, 0, 0, 0).toInstant(gmtOffset)

            return RaceWeekend(
                raceId = raceId,
                openF1MeetingKey = meetingKey,
                raceName = meetingName,
                dateStart = instantStart,
                dateEnd = instantEnd,
                sessions = sessions,
                circuitImage = circuitImage,
                countryName = countryName,
                countryFlag = countryFlag,
                circuitType = circuitType,
                dateLineupOpen = dateLineupOpen,
                dateLineupClose = dateLineupClose
            )
        }
    }
}
