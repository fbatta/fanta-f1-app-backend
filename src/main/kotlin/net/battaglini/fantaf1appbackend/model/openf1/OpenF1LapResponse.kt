package net.battaglini.fantaf1appbackend.model.openf1

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.datetime.LocalDateTime
import net.battaglini.fantaf1appbackend.deserializer.OpenF1TimestampDeserializer
import tools.jackson.databind.annotation.JsonDeserialize

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenF1LapResponse(
    @JsonProperty("date_start")
    @JsonDeserialize(using = OpenF1TimestampDeserializer::class)
    val dateStart: LocalDateTime,
    @JsonProperty("driver_number")
    val driverNumber: Int,
    @JsonProperty("duration_sector_1")
    val durationSector1: Double,
    @JsonProperty("duration_sector_2")
    val durationSector2: Double,
    @JsonProperty("duration_sector_3")
    val durationSector3: Double,
    @JsonProperty("i1_speed")
    val i1Speed: Double,
    @JsonProperty("i2_speed")
    val i2Speed: Double,
    @JsonProperty("is_pit_out_lap")
    val isPitOutLap: Boolean,
    @JsonProperty("lap_duration")
    val lapDuration: Double,
    @JsonProperty("lap_number")
    val lapNumber: Int,
    @JsonProperty("meeting_key")
    val meetingKey: Int,
    @JsonProperty("session_key")
    val sessionKey: Int,
    @JsonProperty("st_speed")
    val speedTrapSpeed: Double
)
