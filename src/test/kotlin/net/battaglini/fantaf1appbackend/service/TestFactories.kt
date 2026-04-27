package net.battaglini.fantaf1appbackend.service

import net.battaglini.fantaf1appbackend.enums.RaceWeekendSessionType
import net.battaglini.fantaf1appbackend.model.Driver
import net.battaglini.fantaf1appbackend.model.RaceWeekend
import net.battaglini.fantaf1appbackend.model.Team
import net.battaglini.fantaf1appbackend.model.User
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Shared test data factories used across service test classes.
 * All functions use sensible defaults and allow overriding specific fields.
 */
object TestFactories {

    fun createRaceWeekend(
        raceId: String = "race1",
        openF1MeetingKey: Int = 100,
        sessions: List<RaceWeekend.Companion.Session> = emptyList(),
        raceName: String = "Test Grand Prix",
        dateStart: Instant = Instant.parse("2025-03-15T10:00:00Z"),
        dateEnd: Instant = Instant.parse("2025-03-17T16:00:00Z"),
        circuitImage: String = "circuit.png",
        countryName: String = "Bahrain",
        countryFlag: String = "\uD83C\uDDEE\uD83C\uDDF7",
        circuitType: String = "street",
        dateLineupOpen: Instant = Instant.parse("2025-03-14T10:00:00Z"),
        dateLineupClose: Instant = Instant.parse("2025-03-15T10:00:00Z")
    ) = RaceWeekend(
        raceId = raceId,
        openF1MeetingKey = openF1MeetingKey,
        raceName = raceName,
        dateStart = dateStart,
        dateEnd = dateEnd,
        sessions = sessions,
        circuitImage = circuitImage,
        countryName = countryName,
        countryFlag = countryFlag,
        circuitType = circuitType,
        dateLineupOpen = dateLineupOpen,
        dateLineupClose = dateLineupClose
    )

    fun createSession(
        sessionId: String = "sess1",
        openF1SessionKey: Int = 1001,
        sessionType: RaceWeekendSessionType = RaceWeekendSessionType.PRACTICE_1,
        dateStart: Instant = Instant.parse("2025-03-15T10:00:00Z"),
        dateEnd: Instant = Instant.parse("2025-03-15T13:00:00Z")
    ) = RaceWeekend.Companion.Session(
        sessionId = sessionId,
        openF1SessionKey = openF1SessionKey,
        sessionType = sessionType,
        dateStart = dateStart,
        dateEnd = dateEnd
    )

    fun createDriver(
        driverId: String = "driver1",
        driverNumber: Int = 1,
        acronym: String = "VER",
        driverAvatar: String = "avatar.png",
        initialCost: Int = 30,
        isActive: Boolean = true,
        name: String = "Max Verstappen",
        teamName: String = "Red Bull Racing",
        teamColour: String = "3671C6"
    ) = Driver(
        driverId = driverId,
        driverNumber = driverNumber,
        acronym = acronym,
        driverAvatar = driverAvatar,
        initialCost = initialCost,
        isActive = isActive,
        name = name,
        teamName = teamName,
        teamColour = teamColour
    )

    fun createTeam(
        teamId: String = "team1",
        ownerId: String = "user1",
        lobbyId: String = "lobby1",
        teamName: String = "My Team",
        teamAvatarUrl: String? = "avatar.png",
        createdAt: Instant = Clock.System.now(),
        updatedAt: Instant = Clock.System.now(),
        points: MutableMap<Int, Double> = mutableMapOf()
    ) = Team(
        teamId = teamId,
        teamName = teamName,
        teamAvatarUrl = teamAvatarUrl,
        ownerId = ownerId,
        lobbyId = lobbyId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        points = points
    )

    fun createUser(
        userId: String = "user1",
        tokens: Map<String, String> = mapOf("fcm_token" to "token_value")
    ) = User(
        userId = userId,
        deviceRegistrationTokens = tokens
    )
}
