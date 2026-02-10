package net.battaglini.fantaf1appbackend.model.openf1

import com.fasterxml.jackson.annotation.JsonProperty

data class OpenF1StartingGridResponse(
    @JsonProperty("driver_number")
    val driverNumber: Int,
    @JsonProperty("lap_duration")
    val lapDuration: Double,
    @JsonProperty("meeting_key")
    val meetingKey: Int,
    val position: Int,
    @JsonProperty("session_key")
    val sessionKey: Int
)
