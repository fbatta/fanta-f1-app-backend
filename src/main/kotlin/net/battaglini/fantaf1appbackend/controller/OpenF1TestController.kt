package net.battaglini.fantaf1appbackend.controller

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.battaglini.fantaf1appbackend.client.OpenF1Client
import net.battaglini.fantaf1appbackend.model.RaceResponse
import net.battaglini.fantaf1appbackend.model.openf1.OpenF1MeetingsResponse.Companion.toRaceResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController("/openf1")
class OpenF1TestController(
    val openF1Client: OpenF1Client
) {
    @GetMapping("/races")
    suspend fun getRaces(@RequestParam("year") year: Int): Flow<RaceResponse> {
        return openF1Client.getRaces(
            year = year,
            meetingKey = null,
            circuitKey = null
        ).map { it.toRaceResponse() }
    }
}