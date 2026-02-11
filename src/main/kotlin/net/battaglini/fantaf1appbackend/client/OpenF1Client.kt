package net.battaglini.fantaf1appbackend.client

import kotlinx.coroutines.flow.Flow
import net.battaglini.fantaf1appbackend.configuration.CacheConfiguration
import net.battaglini.fantaf1appbackend.configuration.CacheConfiguration.Companion.MEETING_SESSIONS_CACHE
import net.battaglini.fantaf1appbackend.configuration.OpenF1ApiProperties
import net.battaglini.fantaf1appbackend.enums.openf1.OpenF1SessionName
import net.battaglini.fantaf1appbackend.enums.openf1.OpenF1TyreCompound
import net.battaglini.fantaf1appbackend.model.openf1.*
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.util.MultiValueMapAdapter
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlow
import org.springframework.web.util.UriBuilder

/**
 * Client for interacting with the OpenF1 API.
 *
 * This client provides methods to retrieve various F1 data such as drivers, races, sessions,
 * results, overtakes, stints, and laps.
 *
 * @property openF1ApiProperties Configuration properties for the OpenF1 API.
 */
@Component
class OpenF1Client(
    openF1ApiProperties: OpenF1ApiProperties
) {
    private val webClient: WebClient = WebClient.builder()
        .baseUrl("${openF1ApiProperties.baseUrl}/${openF1ApiProperties.apiVersion}")
        .build()

    /**
     * Retrieves a list of drivers based on the provided criteria.
     *
     * @param sessionKeys The session keys to filter by.
     * @param meetingKey The meeting key to filter by.
     * @param acronym The driver's acronym to filter by.
     * @param driverNumber The driver's number to filter by.
     * @return A [Flow] emitting [OpenF1DriverResponse] objects.
     * @throws OpenF1ClientRequestException If none of the parameters are provided.
     */
    @Cacheable(CacheConfiguration.DRIVERS_CACHE)
    suspend fun getDrivers(
        sessionKeys: List<Int> = emptyList(),
        meetingKey: Int? = null,
        acronym: String? = null,
        driverNumber: Int? = null
    ): Flow<OpenF1DriverResponse> {
        if (sessionKeys.isEmpty() && meetingKey == null && acronym == null && driverNumber == null) {
            throw OpenF1ClientRequestException("One of sessionKeys, meetingKey, acronym or driverNumber are required")
        }

        return webClient
            .get()
            .uri { uriBuilder ->
                uriBuilder.path("/drivers")
                addMeetingAndSessionKeyParams(uriBuilder, meetingKey, sessionKeys)
                acronym?.also { acronym -> uriBuilder.queryParam("name_acronym", acronym) }
                driverNumber?.also { driverNumber -> uriBuilder.queryParam("driver_number", driverNumber) }

                uriBuilder.build()
            }
            .retrieve()
            .bodyToFlow()
    }

    /**
     * Retrieves a list of races (meetings) based on the provided criteria.
     *
     * @param meetingKey The meeting key to filter by.
     * @param year The year to filter by.
     * @param circuitKey The circuit key to filter by.
     * @return A [Flow] emitting [OpenF1MeetingResponse] objects.
     * @throws OpenF1ClientRequestException If none of the parameters are provided.
     */
    @Cacheable(CacheConfiguration.MEETINGS_CACHE_NAME)
    suspend fun getRaces(
        meetingKey: Int? = null,
        year: Int? = null,
        circuitKey: Int? = null
    ): Flow<OpenF1MeetingResponse> {
        if (meetingKey == null && year == null && circuitKey == null) {
            throw OpenF1ClientRequestException("One of meetingKey, year or circuitKey are required")
        }

        return webClient
            .get()
            .uri { uriBuilder ->
                uriBuilder.path("/meetings")
                addMeetingAndSessionKeyParams(uriBuilder, meetingKey, emptyList())
                year?.also { year -> uriBuilder.queryParam("year", year) }
                circuitKey?.also { circuitKey -> uriBuilder.queryParam("circuit_key", circuitKey) }

                uriBuilder.build()
            }
            .retrieve()
            .bodyToFlow()
    }

    /**
     * Retrieves a list of sessions based on the provided criteria.
     *
     * @param meetingKey The meeting key to filter by.
     * @param sessionKey The session key to filter by.
     * @param sessionName The name of the session to filter by.
     * @param year The year to filter by.
     * @return A [Flow] emitting [OpenF1SessionResponse] objects.
     * @throws OpenF1ClientRequestException If none of the parameters are provided.
     */
    @Cacheable(MEETING_SESSIONS_CACHE)
    suspend fun getSessions(
        meetingKey: Int? = null,
        sessionKey: Int? = null,
        sessionName: OpenF1SessionName? = null,
        year: Int? = null
    ): Flow<OpenF1SessionResponse> {
        if (sessionKey == null && meetingKey == null && year == null && sessionName == null) {
            throw OpenF1ClientRequestException("One of meetingKey, sessionKey, year or sessionName are required")
        }

        return webClient
            .get()
            .uri { uriBuilder ->
                uriBuilder.path("/sessions")
                addMeetingAndSessionKeyParams(uriBuilder, meetingKey, sessionKey?.let { listOf(it) } ?: emptyList())
                year?.also { year -> uriBuilder.queryParam("year", year) }
                sessionName?.also { sessionName -> uriBuilder.queryParam("session_name", sessionName.toString()) }

                uriBuilder.build()
            }
            .retrieve()
            .bodyToFlow()
    }

    /**
     * Retrieves the results of a session.
     *
     * @param meetingKey The meeting key to filter by.
     * @param sessionKeys The session keys to filter by.
     * @return A [Flow] emitting [OpenF1SessionResultResponse] objects.
     * @throws OpenF1ClientRequestException If neither meetingKey nor sessionKey is provided.
     */
    suspend fun <K> getResults(
        meetingKey: Int? = null,
        sessionKeys: List<Int> = emptyList()
    ): Flow<K> {
        if (meetingKey == null && sessionKeys.isEmpty()) {
            throw OpenF1ClientRequestException("One of meetingKey or sessionKey are required")
        }

        return webClient
            .get()
            .uri { uriBuilder ->
                uriBuilder.path("/session_result")
                addMeetingAndSessionKeyParams(uriBuilder, meetingKey, sessionKeys)

                uriBuilder.build()
            }
            .retrieve()
            .bodyToFlow()
    }

    /**
     * Retrieves overtake data based on the provided criteria.
     *
     * @param meetingKey The meeting key to filter by.
     * @param sessionKey The session key to filter by.
     * @param overtakingDriverNumber The number of the overtaking driver.
     * @param overtakenDriverNumber The number of the overtaken driver.
     * @return A [Flow] emitting [OpenF1OvertakeResponse] objects.
     * @throws OpenF1ClientRequestException If none of the parameters are provided.
     */
    suspend fun getOvertakes(
        meetingKey: Int? = null,
        sessionKey: Int? = null,
        overtakingDriverNumber: Int? = null,
        overtakenDriverNumber: Int? = null
    ): Flow<OpenF1OvertakeResponse> {
        if (meetingKey == null && sessionKey == null && overtakenDriverNumber == null && overtakingDriverNumber == null) {
            throw OpenF1ClientRequestException("One of meetingKey, sessionKey, overtakenDriverNumber or overtakingDriverNumber are required")
        }

        return webClient
            .get()
            .uri { uriBuilder ->
                uriBuilder.path("/overtakes")
                addMeetingAndSessionKeyParams(uriBuilder, meetingKey, sessionKey?.let { listOf(it) } ?: emptyList())
                overtakingDriverNumber?.also { overtakingDriverNumber ->
                    uriBuilder.queryParam(
                        "overtaking_driver_number",
                        overtakingDriverNumber
                    )
                }
                overtakenDriverNumber?.also { overtakenDriverNumber ->
                    uriBuilder.queryParam(
                        "overtaken_driver_number",
                        overtakenDriverNumber
                    )
                }

                uriBuilder.build()
            }
            .retrieve()
            .bodyToFlow()
    }

    /**
     * Retrieves stint data based on the provided criteria.
     *
     * @param meetingKey The meeting key to filter by.
     * @param sessionKey The session key to filter by.
     * @param driverNumber The driver number to filter by.
     * @param compound The tyre compound to filter by.
     * @return A [Flow] emitting [OpenF1StintResponse] objects.
     * @throws OpenF1ClientRequestException If none of the parameters are provided.
     */
    suspend fun getStints(
        meetingKey: Int? = null,
        sessionKey: Int? = null,
        driverNumber: Int? = null,
        compound: OpenF1TyreCompound? = null
    ): Flow<OpenF1StintResponse> {
        if (meetingKey == null && sessionKey == null && driverNumber == null && compound == null) {
            throw OpenF1ClientRequestException("One of meetingKey, sessionKey, driverNumber or compound are required")
        }

        return webClient
            .get()
            .uri { uriBuilder ->
                uriBuilder.path("/stints")
                addMeetingAndSessionKeyParams(uriBuilder, meetingKey, sessionKey?.let { listOf(it) } ?: emptyList())
                driverNumber?.also { driverNumber -> uriBuilder.queryParam("driver_number", driverNumber) }
                compound?.also { compound -> uriBuilder.queryParam("compound", compound) }

                uriBuilder.build()
            }
            .retrieve()
            .bodyToFlow()
    }

    /**
     * Retrieves lap data based on the provided criteria.
     *
     * @param meetingKey The meeting key to filter by.
     * @param sessionKey The session key to filter by.
     * @param driverNumber The driver number to filter by.
     * @return A [Flow] emitting [OpenF1LapResponse] objects.
     * @throws OpenF1ClientRequestException If none of the parameters are provided.
     */
    suspend fun getLaps(
        meetingKey: Int? = null,
        sessionKey: Int? = null,
        driverNumber: Int? = null
    ): Flow<OpenF1LapResponse> {
        if (meetingKey == null && sessionKey == null && driverNumber == null) {
            throw OpenF1ClientRequestException("One of meetingKey, sessionKey or driverNumber are required")
        }

        return webClient
            .get()
            .uri { uriBuilder ->
                uriBuilder.path("/laps")
                addMeetingAndSessionKeyParams(uriBuilder, meetingKey, sessionKey?.let { listOf(it) } ?: emptyList())
                driverNumber?.also { driverNumber -> uriBuilder.queryParam("driver_number", driverNumber) }

                uriBuilder.build()
            }
            .retrieve()
            .bodyToFlow()
    }

    /**
     * Retrieves the starting grid for a session.
     *
     * @param meetingKey The meeting key to filter by.
     * @param sessionKey The session key to filter by.
     * @return A [Flow] emitting [OpenF1StartingGridResponse] objects.
     * @throws OpenF1ClientRequestException If neither meetingKey nor sessionKey is provided.
     */
    suspend fun getStartingGrid(
        meetingKey: Int? = null,
        sessionKey: Int? = null,
    ): Flow<OpenF1StartingGridResponse> {
        if (meetingKey == null && sessionKey == null) {
            throw OpenF1ClientRequestException("One of meetingKey or sessionKey are required")
        }

        return webClient
            .get()
            .uri { uriBuilder ->
                uriBuilder.path("/starting_grid")
                addMeetingAndSessionKeyParams(
                    uriBuilder,
                    meetingKey,
                    sessionKey?.let { listOf(sessionKey) } ?: emptyList())

                uriBuilder.build()
            }
            .retrieve()
            .bodyToFlow()
    }

    private fun addMeetingAndSessionKeyParams(uriBuilder: UriBuilder, meetingKey: Int?, sessionKeys: List<Int>) {
        meetingKey?.also { meetingKey -> uriBuilder.queryParam("meeting_key", meetingKey) }
        if (sessionKeys.isNotEmpty()) {
            val sessionKeys = mapOf("session_key" to sessionKeys.map { it.toString() })
            uriBuilder.queryParams(MultiValueMapAdapter(sessionKeys))
        }
    }

    companion object {
        class OpenF1ClientRequestException(message: String) : RuntimeException(message)
    }
}