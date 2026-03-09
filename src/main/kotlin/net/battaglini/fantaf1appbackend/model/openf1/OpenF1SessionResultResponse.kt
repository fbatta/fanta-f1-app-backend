package net.battaglini.fantaf1appbackend.model.openf1

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import net.battaglini.fantaf1appbackend.deserializer.GapToLeaderDeserializer
import tools.jackson.databind.annotation.JsonDeserialize

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenF1SessionResultResponse(
    val position: Int?,
    @JsonProperty("driver_number")
    val driverNumber: Int,
    @JsonProperty("number_of_laps")
    val numberOfLaps: Int,
    val dnf: Boolean,
    val dns: Boolean,
    val dsq: Boolean,
    @JsonProperty("gap_to_leader")
    @JsonDeserialize(using = GapToLeaderDeserializer::class)
    val gapToLeader: Double?,
    val duration: Double?,
    @JsonProperty("meeting_key")
    val meetingKey: Int,
    @JsonProperty("session_key")
    val sessionKey: Int
)
