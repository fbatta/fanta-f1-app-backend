package net.battaglini.fantaf1appbackend.service

import net.battaglini.fantaf1appbackend.model.RaceWeekendResult

interface NotificationService {
    suspend fun processRaceWeekendCalculationCompletedNotification(raceWeekendResult: RaceWeekendResult): Int
}