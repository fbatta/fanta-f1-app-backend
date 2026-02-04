package net.battaglini.fantaf1appbackend.model.openf1

import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.datetime.LocalDateTime
import net.battaglini.fantaf1appbackend.deserializer.OpenF1TimestampDeserializer
import tools.jackson.databind.annotation.JsonDeserialize

data class OpenF1OvertakesResponse(
    @JsonDeserialize(using = OpenF1TimestampDeserializer::class)
    val date: LocalDateTime,
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
