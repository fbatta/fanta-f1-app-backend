package net.battaglini.fantaf1appbackend.service

import net.battaglini.fantaf1appbackend.model.RaceWeekend
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

    /**
     * Sends a notification to all users when a race weekend's lineup is open.
     *
     * @param raceWeekend The race weekend for which the lineup is open.
     * @return The number of notifications successfully sent.
     */
    suspend fun sendLineupOpenNotification(raceWeekend: RaceWeekend): Int
}