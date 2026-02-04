package net.battaglini.fantaf1appbackend.model.openf1

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenF1SessionResultResponse(
    val position: Int,
    @JsonProperty("driver_number")
    val driverNumber: Int,
    @JsonProperty("number_of_laps")
    val numberOfLaps: Int,
    val points: Double,
    val dnf: Boolean,
    val dns: Boolean,
    val dsq: Boolean,
    @JsonProperty("gap_to_leader")
    val gapToLeader: Double,
    @JsonProperty("meeting_key")
    val meetingKey: Int,
    @JsonProperty("session_key")
    val sessionKey: Int
)
