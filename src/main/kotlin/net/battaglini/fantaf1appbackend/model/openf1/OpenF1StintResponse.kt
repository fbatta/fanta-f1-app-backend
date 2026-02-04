package net.battaglini.fantaf1appbackend.model.openf1

import com.fasterxml.jackson.annotation.JsonProperty
import net.battaglini.fantaf1appbackend.enums.openf1.OpenF1TyreCompound

data class OpenF1StintResponse(
    @JsonProperty("meeting_key")
    val meetingKey: Int,
    @JsonProperty("session_key")
    val sessionKey: Int,
    val stint: Int,
    @JsonProperty("driver_number")
    val driverNumber: Int,
    @JsonProperty("stint_number")
    val stintNumber: Int,
    @JsonProperty("lap_start")
    val lapStart: Int,
    @JsonProperty("lap_end")
    val lapEnd: Int,
    val compound: OpenF1TyreCompound,
    @JsonProperty("tyre_age_at_start")
    val tyreAgeAtStart: Double,
)