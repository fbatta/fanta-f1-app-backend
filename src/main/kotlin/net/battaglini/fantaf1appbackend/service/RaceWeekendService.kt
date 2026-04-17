package net.battaglini.fantaf1appbackend.service

/**
 * Service responsible for managing race weekend data, including seeding race schedules.
 */
interface RaceWeekendService {
    /**
     * Fetches current year's race weekend schedule and seeds it into the repository.
     */
    suspend fun seedRaceWeekends()
}