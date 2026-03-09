package net.battaglini.fantaf1appbackend.model.openf1

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenF1OvertakeResponse(
    @JsonProperty("meeting_key")
    val meetingKey: Int,
    @JsonProperty("overtaken_driver_number")
    val overtakenDriverNumber: Int,
    @JsonProperty("overtaking_driver_number")
    val overtakingDriverNumber: Int,
    val position: Int,
    @JsonProperty("session_key")
    val sessionKey: Int
)
