package net.battaglini.fantaf1appbackend.controller

import kotlinx.datetime.plus
import net.battaglini.fantaf1appbackend.exception.InternalServerException
import net.battaglini.fantaf1appbackend.exception.InvalidRequestException
import net.battaglini.fantaf1appbackend.model.request.RecalculateRaceWeekendRequest
import net.battaglini.fantaf1appbackend.model.response.RecalculateRaceWeekendResponse
import net.battaglini.fantaf1appbackend.model.request.GenerateRaceRecapRequest
import net.battaglini.fantaf1appbackend.model.request.OpenRaceWeekendLineupRequest
import net.battaglini.fantaf1appbackend.model.response.GenerateRaceRecapResponse
import net.battaglini.fantaf1appbackend.service.RaceWeekendService
import org.slf4j.LoggerFactory
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.time.Clock

@RestController
@RequestMapping(path = ["/race-weekends"])
class RaceWeekendsController(
    private val raceWeekendService: RaceWeekendService
) {
    @PostMapping("/seed")
    @PreAuthorize("hasRole('ADMIN')")
    suspend fun seedRaceWeekends() {
        try {
            raceWeekendService.seedRaceWeekends()
        } catch (e: Exception) {
            throw RuntimeException(e.message)
        }
    }

    @PostMapping("/open-lineup")
    @PreAuthorize("hasAnyRole('ADMIN', 'RACE_WEEKEND_MANAGER')")
    suspend fun openRaceWeekendLineup(@RequestBody request: OpenRaceWeekendLineupRequest): String {
        try {
            var instant = Clock.System.now()
            if (request.delay != null) {
                request.delayUnit?.let { instant = instant.plus(request.delay, it) } ?: throw InvalidRequestException("delayUnit cannot be null")
            }
            val raceName = raceWeekendService.setRaceWeekendLineupOpenDateTime(request.raceId, instant) ?: throw InternalServerException("Could not update lineup date for raceId=${request.raceId}")
            return raceName
        } catch (e: Exception) {
            return "Could not update lineup date for raceId=${request.raceId}. ${e.message}"
        }
    }

    @PostMapping("/recap")
    @PreAuthorize("hasAnyRole('ADMIN', 'RACE_WEEKEND_MANAGER')")
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
            LOGGER.error("Failed to generate race recap", e)
            throw RuntimeException(e.message)
        }
    }

    @PostMapping("/recalculate")
    @PreAuthorize("hasAnyRole('ADMIN', 'DRIVERS_MANAGER')")
    suspend fun recalculateRaceWeekend(@RequestBody request: RecalculateRaceWeekendRequest): RecalculateRaceWeekendResponse {
        try {
            val response = raceWeekendService.recalculateRaceWeekend(request.raceId)
            return response
        } catch (e: Exception) {
            LOGGER.error("Failed to recalculate race weekend for raceId={}", request.raceId, e)
            throw RuntimeException(e.message)
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(RaceWeekendsController::class.java)
    }
}
