package net.battaglini.fantaf1appbackend.client

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import net.battaglini.fantaf1appbackend.configuration.CacheConfiguration
import net.battaglini.fantaf1appbackend.configuration.CacheConfiguration.Companion.MEETING_SESSIONS_CACHE
import net.battaglini.fantaf1appbackend.configuration.OpenF1ApiProperties
import net.battaglini.fantaf1appbackend.enums.openf1.OpenF1SessionName
import net.battaglini.fantaf1appbackend.enums.openf1.OpenF1TyreCompound
import net.battaglini.fantaf1appbackend.model.openf1.*
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.util.MultiValueMapAdapter
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlow
import org.springframework.web.reactive.function.client.exchangeToFlow
import org.springframework.web.util.UriBuilder

/**
 * Implementation of OpenF1Client for interacting with the OpenF1 API.
 *
 * @property rateLimiter Rate limiter for API rate limiting.
 * @property openF1ApiProperties Configuration properties for the OpenF1 API.
 */
class OpenF1ClientRequestException(message: String) : RuntimeException(message)

@Component
class OpenF1ClientImpl(
    private val rateLimiter: OpenF1RateLimiter,
    openF1ApiProperties: OpenF1ApiProperties
) : OpenF1Client {

    private val webClient: WebClient = WebClient.builder()
        .baseUrl("${openF1ApiProperties.baseUrl}/${openF1ApiProperties.apiVersion}")
        .build()

    private suspend fun <T : Any> rateLimited(block: suspend () -> Flow<T>): Flow<T> {
        rateLimiter.acquire()
        return flow { emitAll(block()) }
    }

    @Cacheable(CacheConfiguration.DRIVERS_CACHE)
    override suspend fun getDrivers(
        sessionKeys: List<String>,
        meetingKey: Int?,
        acronym: String?,
        driverNumber: Int?
    ): Flow<OpenF1DriverResponse> {
        if (sessionKeys.isEmpty() && meetingKey == null && acronym == null && driverNumber == null) {
            throw OpenF1ClientRequestException("One of sessionKeys, meetingKey, acronym or driverNumber are required")
        }

        return rateLimited {
            webClient
                .get()
                .uri { uriBuilder ->
                    uriBuilder.path("/drivers")
                    addMeetingAndSessionKeyParams(uriBuilder, meetingKey, sessionKeys)
                    acronym?.also { acronym -> uriBuilder.queryParam("name_acronym", acronym) }
                    driverNumber?.also { driverNumber -> uriBuilder.queryParam("driver_number", driverNumber) }

                    uriBuilder.build()
                }
                .exchangeToFlow { it.bodyToFlow() }
        }
    }

    @Cacheable(CacheConfiguration.MEETINGS_CACHE_NAME)
    override suspend fun getRaces(
        meetingKey: Int?,
        year: Int?,
        circuitKey: Int?
    ): Flow<OpenF1MeetingResponse> {
        if (meetingKey == null && year == null && circuitKey == null) {
            throw OpenF1ClientRequestException("One of meetingKey, year or circuitKey are required")
        }

        return rateLimited {
            webClient
                .get()
                .uri { uriBuilder ->
                    uriBuilder.path("/meetings")
                    addMeetingAndSessionKeyParams(uriBuilder, meetingKey, emptyList())
                    year?.also { year -> uriBuilder.queryParam("year", year) }
                    circuitKey?.also { circuitKey -> uriBuilder.queryParam("circuit_key", circuitKey) }

                    uriBuilder.build()
                }
                .exchangeToFlow { it.bodyToFlow() }
        }
    }

    @Cacheable(MEETING_SESSIONS_CACHE)
    override suspend fun getSessions(
        meetingKey: Int?,
        sessionKey: String?,
        sessionName: OpenF1SessionName?,
        year: Int?
    ): Flow<OpenF1SessionResponse> {
        if (sessionKey == null && meetingKey == null && year == null && sessionName == null) {
            throw OpenF1ClientRequestException("One of meetingKey, sessionKey, year or sessionName are required")
        }

        return rateLimited {
            webClient
                .get()
                .uri { uriBuilder ->
                    uriBuilder.path("/sessions")
                    addMeetingAndSessionKeyParams(uriBuilder, meetingKey, sessionKey?.let { listOf(it) } ?: emptyList())
                    year?.also { year -> uriBuilder.queryParam("year", year) }
                    sessionName?.also { sessionName -> uriBuilder.queryParam("session_name", sessionName.toString()) }

                    uriBuilder.build()
                }
                .exchangeToFlow { it.bodyToFlow() }
        }
    }

    override suspend fun getResults(
        meetingKey: Int?,
        sessionKeys: List<String>
    ): Flow<OpenF1SessionResultResponse> {
        if (meetingKey == null && sessionKeys.isEmpty()) {
            throw OpenF1ClientRequestException("One of meetingKey or sessionKey are required")
        }

        return rateLimited {
            webClient
                .get()
                .uri { uriBuilder ->
                    uriBuilder.path("/session_result")
                    addMeetingAndSessionKeyParams(uriBuilder, meetingKey, sessionKeys)

                    uriBuilder.build()
                }
                .exchangeToFlow {
                    if (it.statusCode() != HttpStatus.OK) {
                        return@exchangeToFlow emptyFlow()
                    }
                    it.bodyToFlow()
                }
        }
    }

    override suspend fun getQualifyingResults(
        meetingKey: Int?,
        sessionKeys: List<String>
    ): Flow<OpenF1QualifyingSessionResultResponse> {
        if (meetingKey == null && sessionKeys.isEmpty()) {
            throw OpenF1ClientRequestException("One of meetingKey or sessionKey are required")
        }

        return rateLimited {
            webClient
                .get()
                .uri { uriBuilder ->
                    uriBuilder.path("/session_result")
                    addMeetingAndSessionKeyParams(uriBuilder, meetingKey, sessionKeys)

                    uriBuilder.build()
                }
                .exchangeToFlow {
                    if (it.statusCode() != HttpStatus.OK) {
                        return@exchangeToFlow emptyFlow()
                    }
                    it.bodyToFlow()
                }
        }
    }

    override suspend fun getOvertakes(
        meetingKey: Int?,
        sessionKey: String?,
        overtakingDriverNumber: Int?,
        overtakenDriverNumber: Int?
    ): Flow<OpenF1OvertakeResponse> {
        if (meetingKey == null && sessionKey == null && overtakenDriverNumber == null && overtakingDriverNumber == null) {
            throw OpenF1ClientRequestException("One of meetingKey, sessionKey, overtakenDriverNumber or overtakingDriverNumber are required")
        }

        return rateLimited {
            webClient
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
                .exchangeToFlow {
                    if (it.statusCode() == HttpStatus.NOT_FOUND) {
                        return@exchangeToFlow emptyFlow()
                    }
                    it.bodyToFlow()
                }
        }
    }

    override suspend fun getStints(
        meetingKey: Int?,
        sessionKey: String?,
        driverNumber: Int?,
        compound: OpenF1TyreCompound?
    ): Flow<OpenF1StintResponse> {
        if (meetingKey == null && sessionKey == null && driverNumber == null && compound == null) {
            throw OpenF1ClientRequestException("One of meetingKey, sessionKey, driverNumber or compound are required")
        }

        return rateLimited {
            webClient
                .get()
                .uri { uriBuilder ->
                    uriBuilder.path("/stints")
                    addMeetingAndSessionKeyParams(uriBuilder, meetingKey, sessionKey?.let { listOf(it) } ?: emptyList())
                    driverNumber?.also { driverNumber -> uriBuilder.queryParam("driver_number", driverNumber) }
                    compound?.also { compound -> uriBuilder.queryParam("compound", compound) }

                    uriBuilder.build()
                }
                .exchangeToFlow { it.bodyToFlow() }
        }
    }

    override suspend fun getLaps(
        meetingKey: Int?,
        sessionKey: String?,
        driverNumber: Int?
    ): Flow<OpenF1LapResponse> {
        if (meetingKey == null && sessionKey == null && driverNumber == null) {
            throw OpenF1ClientRequestException("One of meetingKey, sessionKey or driverNumber are required")
        }

        return rateLimited {
            webClient
                .get()
                .uri { uriBuilder ->
                    uriBuilder.path("/laps")
                    addMeetingAndSessionKeyParams(uriBuilder, meetingKey, sessionKey?.let { listOf(it) } ?: emptyList())
                    driverNumber?.also { driverNumber -> uriBuilder.queryParam("driver_number", driverNumber) }

                    uriBuilder.build()
                }
                .exchangeToFlow { response ->
                    if (response.statusCode() == HttpStatus.NOT_FOUND) {
                        return@exchangeToFlow emptyFlow()
                    }
                    response.bodyToFlow()
                }
        }
    }

    private fun addMeetingAndSessionKeyParams(uriBuilder: UriBuilder, meetingKey: Int?, sessionKeys: List<String>) {
        meetingKey?.also { meetingKey -> uriBuilder.queryParam("meeting_key", meetingKey) }
        if (sessionKeys.isNotEmpty()) {
            val sessionKeys = mapOf("session_key" to sessionKeys)
            uriBuilder.queryParams(MultiValueMapAdapter(sessionKeys))
        }
    }
}
