package net.battaglini.fantaf1appbackend.deserializer

import kotlinx.datetime.UtcOffset
import kotlinx.datetime.format.char
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.deser.std.StdDeserializer

class OpenF1GmtOffsetDeserializer : StdDeserializer<UtcOffset>(String::class.java) {
    override fun deserialize(
        p: JsonParser?,
        ctxt: DeserializationContext?
    ): UtcOffset? {
        p?.string?.let { it.startsWith('-').not().apply { it.padStart(1, '+') } }
        return p?.string
            ?.let { str ->
                if (!str.startsWith('-'))
                    return@let "+$str"
                return@let str
            }
            ?.let { UtcOffset.parse(it, customFormat) }
    }

    companion object {
        val customFormat = UtcOffset.Format {
            offsetHours(); char(':'); offsetMinutesOfHour(); char(':'); offsetSecondsOfMinute()
        }
    }
}