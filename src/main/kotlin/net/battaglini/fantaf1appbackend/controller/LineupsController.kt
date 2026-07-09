package net.battaglini.fantaf1appbackend.controller

import net.battaglini.fantaf1appbackend.exception.InvalidRequestException
import net.battaglini.fantaf1appbackend.model.request.CreateLineupRequest
import net.battaglini.fantaf1appbackend.model.response.CreateLineupResponse
import net.battaglini.fantaf1appbackend.service.LineupService
import org.slf4j.LoggerFactory
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/lineups"])
@PreAuthorize("hasAnyRole('ADMIN', 'DRIVERS_MANAGER')")
class LineupsController(
    private val lineupService: LineupService
) {
    @PostMapping
    suspend fun createLineup(@RequestBody request: CreateLineupRequest): CreateLineupResponse {
        try {
            return lineupService.createLineup(request)
        } catch (e: InvalidRequestException) {
            throw e
        } catch (e: Exception) {
            LOGGER.error("Failed to create lineup for teamId={}, raceId={}", request.teamId, request.raceId, e)
            throw RuntimeException(e.message)
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(LineupsController::class.java)
    }
}
