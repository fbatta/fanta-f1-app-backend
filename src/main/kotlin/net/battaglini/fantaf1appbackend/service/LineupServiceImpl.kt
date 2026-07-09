package net.battaglini.fantaf1appbackend.service

import net.battaglini.fantaf1appbackend.exception.InvalidRequestException
import net.battaglini.fantaf1appbackend.model.Lineup
import net.battaglini.fantaf1appbackend.model.request.CreateLineupRequest
import net.battaglini.fantaf1appbackend.model.response.CreateLineupResponse
import net.battaglini.fantaf1appbackend.model.response.CreateLineupResponse.LineupDriverDto
import net.battaglini.fantaf1appbackend.repository.DriverCostRepository
import net.battaglini.fantaf1appbackend.repository.DriverRepository
import net.battaglini.fantaf1appbackend.repository.LineupRepository
import net.battaglini.fantaf1appbackend.repository.TeamRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.time.Clock

@Service
class LineupServiceImpl(
    private val clock: Clock,
    private val raceWeekendService: RaceWeekendService,
    private val teamRepository: TeamRepository,
    private val driverRepository: DriverRepository,
    private val driverCostRepository: DriverCostRepository,
    private val lineupRepository: LineupRepository
) : LineupService {
    override suspend fun createLineup(request: CreateLineupRequest): CreateLineupResponse {
        val raceWeekend = raceWeekendService.getRaceWeekend(request.raceId)
            ?: throw InvalidRequestException("RaceWeekend with raceId=${request.raceId} not found")

        val team = teamRepository.getTeamByTeamId(request.teamId)
            ?: throw InvalidRequestException("Team with teamId=${request.teamId} not found")

        if (request.driverIds.isEmpty()) {
            throw InvalidRequestException("Driver IDs list cannot be empty")
        }

        val now = clock.now()
        val lineupId = "lineup-${request.teamId}-${request.raceId}"

        val lineupDrivers = request.driverIds.map { driverId ->
            val driver = driverRepository.findDriverById(driverId)
                ?: throw InvalidRequestException("Driver with driverId=$driverId not found")

            val driverCost = driverCostRepository.getDriverCostByDriverId(driverId)
                ?: throw InvalidRequestException("Driver cost not found for driverId=$driverId")

            LineupDriverDto(
                driverId = driverId,
                driverNumber = driver.driverNumber,
                driverAcronym = driver.acronym,
                driverCost = driverCost.driverCost
            )
        }

        val lineup = Lineup(
            lineupId = lineupId,
            teamId = request.teamId,
            ownerId = team.ownerId,
            raceId = request.raceId,
            drivers = lineupDrivers.map {
                Lineup.Companion.LineupDriver(
                    driverId = it.driverId,
                    driverNumber = it.driverNumber,
                    driverAcronym = it.driverAcronym,
                    driverCost = it.driverCost
                )
            },
            createdAt = now,
            updatedAt = now,
            version = 1,
            score = null
        )

        lineupRepository.createOrUpdateLineup(lineup)

        return CreateLineupResponse(
            lineupId = lineupId,
            teamId = request.teamId,
            raceId = request.raceId,
            drivers = lineupDrivers
        )
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(LineupServiceImpl::class.java)
    }
}
