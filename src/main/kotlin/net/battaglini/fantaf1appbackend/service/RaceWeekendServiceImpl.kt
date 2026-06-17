package net.battaglini.fantaf1appbackend.service

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.battaglini.fantaf1appbackend.client.OpenF1Client
import net.battaglini.fantaf1appbackend.configuration.SeedingProperties
import net.battaglini.fantaf1appbackend.model.CombinedDriversRaceWeekendResults
import net.battaglini.fantaf1appbackend.model.RaceWeekend
import net.battaglini.fantaf1appbackend.model.RaceWeekendRecap
import net.battaglini.fantaf1appbackend.model.RaceWeekendResult
import net.battaglini.fantaf1appbackend.model.openf1.OpenF1MeetingResponse.Companion.toRace
import net.battaglini.fantaf1appbackend.model.openf1.OpenF1SessionResponse.Companion.toRaceWeekendSession
import net.battaglini.fantaf1appbackend.model.response.RecalculateRaceWeekendResponse
import net.battaglini.fantaf1appbackend.repository.RaceRepository
import net.battaglini.fantaf1appbackend.repository.RaceWeekendRecapRepository
import net.battaglini.fantaf1appbackend.repository.RaceWeekendResultRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Service
class RaceWeekendServiceImpl(
    private val openF1Client: OpenF1Client,
    private val raceRepository: RaceRepository,
    private val seedingProperties: SeedingProperties,
    private val genAIService: GenAIService,
    private val raceWeekendRecapRepository: RaceWeekendRecapRepository,
    private val raceWeekendResultRepository: RaceWeekendResultRepository,
    private val clock: Clock,
    private val timeZone: TimeZone,
    private val practiceResultsService: net.battaglini.fantaf1appbackend.service.PracticeResultsService,
    private val qualifyingResultsService: net.battaglini.fantaf1appbackend.service.QualifyingResultsService,
    private val raceResultsService: net.battaglini.fantaf1appbackend.service.RaceResultsService,
    private val raceWeekendResultsCalculator: net.battaglini.fantaf1appbackend.service.RaceWeekendResultsCalculator
) : RaceWeekendService {
    @EventListener(ApplicationStartedEvent::class)
    private suspend fun onStart() {
        if (!seedingProperties.raceWeekends) {
            LOGGER.info("Skipping F1 race weekends' seeding because it is disabled in app config")
        } else {
            seedRaceWeekends()
        }
    }

    override suspend fun seedRaceWeekends() {
        try {
            LOGGER.info("Seeding F1 race weekends...")
            val year = clock.now().toLocalDateTime(timeZone).year

            val races = openF1Client.getRaces(year = year).map { meeting ->
                val sessions = openF1Client.getSessions(meeting.meetingKey)
                    .map { it.toRaceWeekendSession(calculateSessionId(it.sessionKey, it.meetingKey)) }
                delay(100.toDuration(DurationUnit.MILLISECONDS))
                meeting.toRace(calculateRaceId(meeting.meetingKey, meeting.year), sessions.toList())
            }.toList()
            raceRepository.createOrUpdateRaces(races)

            LOGGER.info("Seeded {} race weekends into Firebase", races.size)
        } catch (e: Exception) {
            LOGGER.error("Error seeding race weekends into Firebase", e)
        }
    }

    override suspend fun getRaceWeekend(raceId: String): RaceWeekend? {
        return raceRepository.getRaceById(raceId).firstOrNull()
    }

    override suspend fun getRaceWeekendResults(raceId: String): RaceWeekendResult? {
        return raceWeekendResultRepository.findRaceWeekendResult(raceId = raceId)
    }

    override suspend fun generateRaceRecap(raceIds: List<String>): List<RaceWeekendRecap> {
        val processedRecaps = mutableListOf<RaceWeekendRecap>()

        for (raceId in raceIds) {
            try {
                val race = raceRepository.getRaceById(raceId).first()

                val recapFlow = genAIService.generateRaceRecap(race.raceName)
                val paragraphs = recapFlow.toList()

                val recap = RaceWeekendRecap(
                    raceId = raceId,
                    raceName = race.raceName,
                    recapParagraphs = paragraphs
                )

                raceWeekendRecapRepository.saveRaceWeekendRecap(recap)
                processedRecaps.add(recap)

                LOGGER.info("Generated race recap for $raceId ($race.raceName)")
            } catch (e: Exception) {
                LOGGER.error("Failed to generate race recap for $raceId", e)
            }
        }

        return processedRecaps
    }

    override suspend fun fetchDriversResults(raceWeekend: RaceWeekend): CombinedDriversRaceWeekendResults? {
        val combinedPracticeResults =
            fetchResults { practiceResultsService.getDriversResultsForCombinedPractice(raceWeekend) }
        val qualifyingResults =
            fetchResults { qualifyingResultsService.getDriversResultsForQualifying(raceWeekend, false) }
        val sprintQualifyingResults =
            fetchResults { qualifyingResultsService.getDriversResultsForQualifying(raceWeekend, true) }
        val raceResults = fetchResults { raceResultsService.getResultsForRace(raceWeekend, false) }
        val sprintRaceResults = fetchResults { raceResultsService.getResultsForRace(raceWeekend, true) }

        if (combinedPracticeResults.isEmpty() || qualifyingResults.isEmpty() || raceResults.isEmpty()) {
            LOGGER.warn(
                "Could not calculate minimum set of results for raceId={}, raceName={}. Results might not be available yet",
                raceWeekend.raceId,
                raceWeekend.raceName
            )
            return null
        }

        return CombinedDriversRaceWeekendResults(
            raceId = raceWeekend.raceId,
            raceResults = raceResults,
            sprintRaceResults = sprintRaceResults,
            sprintQualifyingResults = sprintQualifyingResults,
            qualifyingResults = qualifyingResults,
            combinedPracticeResults = combinedPracticeResults
        )
    }

    override suspend fun recalculateRaceWeekend(raceId: String): RecalculateRaceWeekendResponse {
        val raceWeekend = raceRepository.getRaceById(raceId).firstOrNull()
            ?: throw RuntimeException("Race weekend with raceId=$raceId not found")

        val combinedDriversResults = fetchDriversResults(raceWeekend)
            ?: throw IllegalStateException("Could not recalculate raceWeekend results for raceId=$raceId because it did not have the minimum required set of results")

        val newResult = raceWeekendResultsCalculator.calculateRaceWeekendResults(
            combinedDriversResults.combinedPracticeResults,
            combinedDriversResults.qualifyingResults,
            combinedDriversResults.sprintQualifyingResults,
            combinedDriversResults.raceResults,
            combinedDriversResults.sprintRaceResults,
            raceWeekend
        )

        val existingResult = raceWeekendResultRepository.findRaceWeekendResult(raceId = raceId)
        val newVersion = (existingResult?.version ?: 0) + 1

        val updatedResult = newResult.copy(
            version = newVersion,
            updatedAt = clock.now(),
            createdAt = existingResult?.createdAt ?: clock.now()
        )

        raceWeekendResultRepository.updateRaceWeekendResult(updatedResult)

        return RecalculateRaceWeekendResponse(
            raceId = raceId,
            raceName = raceWeekend.raceName,
            version = newVersion,
            oldResults = existingResult,
            newResults = newResult,
        )
    }

    private suspend fun <T> fetchResults(fetcher: suspend () -> Flow<T>): List<T> {
        val results = fetcher().toList()
        delay(2.seconds)
        return results
    }

    private fun calculateRaceId(meetingKey: Int, year: Int): String {
        return Uuid.fromULongs(meetingKey.toULong(), year.toULong()).toString()
    }

    private fun calculateSessionId(sessionKey: Int, meetingKey: Int): String {
        return Uuid.fromULongs(sessionKey.toULong(), meetingKey.toULong()).toString()
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(RaceWeekendServiceImpl::class.java)
    }
}
