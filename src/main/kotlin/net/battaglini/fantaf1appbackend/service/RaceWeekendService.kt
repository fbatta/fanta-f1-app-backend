package net.battaglini.fantaf1appbackend.service

/**
 * Service responsible for managing race weekend data, including seeding race schedules.
 */
interface RaceWeekendService {
    /**
     * Fetches current year's race weekend schedule and seeds it into the repository.
     */
    suspend fun seedRaceWeekends()

    /**
     * Generates AI race recaps for the given race IDs.
     * Each recap is generated via GenAI and saved to Firestore.
     *
     * @param raceIds list of race IDs to generate recaps for
     * @return list of successfully processed race recaps with generated content
      */
    suspend fun generateRaceRecap(raceIds: List<String>): List<net.battaglini.fantaf1appbackend.model.RaceWeekendRecap>
}