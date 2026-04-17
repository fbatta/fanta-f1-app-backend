package net.battaglini.fantaf1appbackend.service

import net.battaglini.fantaf1appbackend.model.RaceWeekendResult

/**
 * Service responsible for sending push notifications to users.
 */
interface NotificationService {
    /**
     * Processes and sends notifications to all relevant users when a race weekend's
     * results have been calculated and are available.
     *
     * @param raceWeekendResult The calculated results for the race weekend.
     * @return The number of notifications successfully sent.
     */
    suspend fun processRaceWeekendCalculationCompletedNotification(raceWeekendResult: RaceWeekendResult): Int
}