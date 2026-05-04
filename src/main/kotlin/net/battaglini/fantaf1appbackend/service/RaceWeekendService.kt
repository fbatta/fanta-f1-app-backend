package net.battaglini.fantaf1appbackend.service

import net.battaglini.fantaf1appbackend.model.RaceWeekend
import net.battaglini.fantaf1appbackend.model.RaceWeekendRecap
import net.battaglini.fantaf1appbackend.model.RaceWeekendResult

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
     * Retrieves a race weekend by its unique race ID.
     *
     * @param raceId the unique identifier of the race weekend
     * @return the [RaceWeekend] if found, null otherwise
     */
    suspend fun getRaceWeekend(raceId: String): RaceWeekend?

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
}