package net.battaglini.fantaf1appbackend.controller

import net.battaglini.fantaf1appbackend.exception.InvalidRequestException
import net.battaglini.fantaf1appbackend.model.request.CalculateTeamsResultsRequest
import net.battaglini.fantaf1appbackend.model.response.CalculateTeamsResultsResponse
import net.battaglini.fantaf1appbackend.service.RaceWeekendService
import net.battaglini.fantaf1appbackend.service.TeamResultsService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/teams"])
class TeamsController(
    private val raceWeekendService: RaceWeekendService,
    private val teamResultsService: TeamResultsService
) {
    @PostMapping("/results")
    suspend fun calculateTeamsResults(@RequestBody request: CalculateTeamsResultsRequest): CalculateTeamsResultsResponse {
        try {
            val raceWeekend = raceWeekendService.getRaceWeekend(request.raceId)
                ?: throw InvalidRequestException("RaceWeekend with raceId=${request.raceId} not found")
            val raceWeekendResults = raceWeekendService.getRaceWeekendResults(request.raceId)
                ?: throw InvalidRequestException("No results found for raceId=${request.raceId}")

            val teamsResults = teamResultsService.calculateAndSaveLineupsResults(raceWeekendResults)

            return CalculateTeamsResultsResponse(
                raceId = raceWeekend.raceId,
                raceName = raceWeekend.raceName,
                scores = teamsResults.map { CalculateTeamsResultsResponse.Companion.TeamScore(it.key, it.value) }
            )
        } catch (e: Exception) {
            LOGGER.error("Failed to calculate teams results for raceId={}", request.raceId, e)
            throw RuntimeException(e.message)
        }
    }

    companion object {
        private val LOGGER = org.slf4j.LoggerFactory.getLogger(TeamsController::class.java)
    }
}