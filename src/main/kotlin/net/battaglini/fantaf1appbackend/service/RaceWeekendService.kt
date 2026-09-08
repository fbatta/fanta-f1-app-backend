package net.battaglini.fantaf1appbackend.service

import net.battaglini.fantaf1appbackend.model.CombinedDriversRaceWeekendResults
import net.battaglini.fantaf1appbackend.model.RaceWeekend
import net.battaglini.fantaf1appbackend.model.RaceWeekendRecap
import net.battaglini.fantaf1appbackend.model.RaceWeekendResult
import net.battaglini.fantaf1appbackend.model.response.RecalculateRaceWeekendResponse
import kotlin.time.Instant

/**
 * Service responsible for managing race weekend data, including seeding race schedules,
 * retrieving race weekend details, and generating AI-powered race recaps.
 */
interface RaceWeekendService {
    /**
     * Fetches current year's race weekend schedule from the OpenF1 API and seeds it into Firestore.
     * Retrieves meetings and their associated sessions, then persists them to the repository.
     */
    suspend fun seedRaceWeekends()

    /**
     * Fetches all the drivers' results for a specific race weekend, needed to then
     * calculate each driver's points for that weekend
     */
    suspend fun fetchDriversResults(raceWeekend: RaceWeekend): CombinedDriversRaceWeekendResults?

    /**
     * Retrieves a race weekend by its unique race ID.
     *
     * @param raceId the unique identifier of the race weekend
     * @return the [RaceWeekend] if found, null otherwise
     */
    suspend fun getRaceWeekend(raceId: String): RaceWeekend?

    /**
     * Update the timestamp at which the lineup opens for a specific race
     *
     * @param raceId the unique identifier of the race weekend
     * @param instant the value to set as the lineup open timestamp
     * @return [String] containing the race name
     */
    suspend fun setRaceWeekendLineupOpenDateTime(raceId: String, instant: Instant): String?

    /**
     * Retrieves the calculated results for a race weekend by its race ID.
     *
     * @param raceId the unique identifier of the race weekend
     * @return the [RaceWeekendResult] if found, null otherwise
     */
    suspend fun getRaceWeekendResults(raceId: String): RaceWeekendResult?

    /**
     * Generates AI-powered race recaps for the given race IDs.
     * Each recap is generated via GenAI using the race name, then saved to Firestore.
     * Races that fail to process are logged and skipped; processing continues for remaining races.
     *
     * @param raceIds list of race IDs to generate recaps for
     * @return list of successfully processed race recaps with generated content
     */
    suspend fun generateRaceRecap(raceIds: List<String>): List<RaceWeekendRecap>
    suspend fun recalculateRaceWeekend(raceId: String): RecalculateRaceWeekendResponse
}