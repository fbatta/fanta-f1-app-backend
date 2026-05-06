package net.battaglini.fantaf1appbackend.controller

import net.battaglini.fantaf1appbackend.model.request.GenerateRaceRecapRequest
import net.battaglini.fantaf1appbackend.model.response.GenerateRaceRecapResponse
import net.battaglini.fantaf1appbackend.service.RaceWeekendService
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/race-weekends"])
class RaceWeekendsController(
    private val raceWeekendService: RaceWeekendService
) {
    @PostMapping("/seed")
    suspend fun seedRaceWeekends() {
        try {
            raceWeekendService.seedRaceWeekends()
        } catch (e: Exception) {
            throw RuntimeException(e.message)
        }
    }

    @PostMapping("/recap")
    suspend fun generateRaceRecaps(@RequestBody request: GenerateRaceRecapRequest): GenerateRaceRecapResponse {
        try {
            val recaps = raceWeekendService.generateRaceRecap(request.raceIds)
            val recapEntries = recaps.map {
                GenerateRaceRecapResponse.RecapEntry(
                    raceId = it.raceId,
                    raceName = it.raceName,
                    recapParagraphs = it.recapParagraphs
                )
            }
            return GenerateRaceRecapResponse(
                recapIds = recaps.map { it.raceId },
                recaps = recapEntries
            )
        } catch (e: Exception) {
            LOGGER.error("Failed to generate race recaps", e)
            throw RuntimeException(e.message)
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(RaceWeekendsController::class.java)
    }
}