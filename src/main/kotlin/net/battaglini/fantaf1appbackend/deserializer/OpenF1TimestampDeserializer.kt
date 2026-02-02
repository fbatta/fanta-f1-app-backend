package net.battaglini.fantaf1appbackend.deserializer

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.char
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.deser.std.StdDeserializer

class OpenF1TimestampDeserializer : StdDeserializer<LocalDateTime>(String::class.java) {
    override fun deserialize(
        p: JsonParser?,
        ctxt: DeserializationContext?
    ): LocalDateTime? {
        return p?.string?.let { LocalDateTime.parse(it, timestampFormat) }
    }

    companion object {
        val timestampFormat = LocalDateTime.Format {
            date(LocalDate.Formats.ISO)
            char('T')
            hour(); char(':'); minute(); char(':'); second()
            chars("+00:00")
        }
    }
}