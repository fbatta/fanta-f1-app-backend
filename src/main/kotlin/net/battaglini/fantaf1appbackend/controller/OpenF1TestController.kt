package net.battaglini.fantaf1appbackend.controller

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.battaglini.fantaf1appbackend.client.OpenF1Client
import net.battaglini.fantaf1appbackend.model.RaceWeekend
import net.battaglini.fantaf1appbackend.model.openf1.OpenF1MeetingResponse.Companion.toRace
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@RestController("/openf1")
class OpenF1TestController(
    val openF1Client: OpenF1Client
) {
    @GetMapping("/races")
    suspend fun getRaces(@RequestParam("year") year: Int): Flow<RaceWeekend> {
        return openF1Client.getRaces(
            year = year,
            meetingKey = null,
            circuitKey = null
        ).map { it.toRace(raceId = Uuid.random().toString()) }
    }
}