package net.battaglini.fantaf1appbackend.model.openf1

import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.UtcOffset
import net.battaglini.fantaf1appbackend.deserializer.OpenF1GmtOffsetDeserializer
import net.battaglini.fantaf1appbackend.deserializer.OpenF1TimestampDeserializer
import net.battaglini.fantaf1appbackend.enums.openf1.OpenF1SessionName
import net.battaglini.fantaf1appbackend.enums.openf1.OpenF1SessionType
import tools.jackson.databind.annotation.JsonDeserialize

data class OpenF1SessionResponse(
    @JsonProperty("meeting_key")
    val meetingKey: Int,
    @JsonProperty("session_key")
    val sessionKey: Int,
    @JsonProperty("session_name")
    @JsonDeserialize(using = OpenF1SessionName.Companion.Deserializer::class)
    val sessionName: OpenF1SessionName,
    @JsonProperty("session_type")
    @JsonDeserialize(using = OpenF1SessionType.Companion.Deserializer::class)
    val sessionType: OpenF1SessionType,
    @JsonProperty("date_start")
    @JsonDeserialize(using = OpenF1TimestampDeserializer::class)
    val dateStart: LocalDateTime,
    @JsonProperty("date_end")
    @JsonDeserialize(using = OpenF1TimestampDeserializer::class)
    val dateEnd: LocalDateTime,
    @JsonProperty("gmt_offset")
    @JsonDeserialize(using = OpenF1GmtOffsetDeserializer::class)
    val gmtOffset: UtcOffset,
    val year: Int
)
