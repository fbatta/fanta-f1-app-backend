package net.battaglini.fantaf1appbackend.client

import kotlinx.coroutines.flow.Flow
import net.battaglini.fantaf1appbackend.configuration.OpenF1ApiProperties
import net.battaglini.fantaf1appbackend.enums.openf1.OpenF1TyreCompound
import net.battaglini.fantaf1appbackend.model.openf1.*
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlow
import org.springframework.web.util.UriBuilder

@Component
class OpenF1Client(
    openF1ApiProperties: OpenF1ApiProperties
) {
    private val webClient: WebClient = WebClient.builder()
        .baseUrl("${openF1ApiProperties.baseUrl}/${openF1ApiProperties.apiVersion}")
        .build()

    suspend fun getDrivers(
        sessionKey: Int? = null,
        meetingKey: Int? = null,
        acronym: String? = null,
        driverNumber: Int? = null
    ): Flow<OpenF1DriverResponse> {
        return webClient
            .get()
            .uri { uriBuilder ->
                uriBuilder.path("/drivers")
                    .queryParam("session_key", sessionKey ?: "latest")
                    .queryParam("meeting_key", meetingKey ?: "latest")
                acronym?.also { acronym -> uriBuilder.queryParam("name_acronym", acronym) }
                driverNumber?.also { driverNumber -> uriBuilder.queryParam("driver_number", driverNumber) }

                uriBuilder.build()
            }
            .retrieve()
            .bodyToFlow()
    }

    suspend fun getRaces(
        meetingKey: Int?,
        year: Int?,
        circuitKey: Int?
    ): Flow<OpenF1MeetingResponse> {
        if (meetingKey == null && year == null && circuitKey == null) {
            throw OpenF1ClientRequestException("One of meetingKey, year or circuitKey are required")
        }

        return webClient
            .get()
            .uri { uriBuilder ->
                uriBuilder.path("/meetings")
                addMeetingAndSessionKeyParams(uriBuilder, meetingKey, null)
                year?.also { year -> uriBuilder.queryParam("year", year) }
                circuitKey?.also { circuitKey -> uriBuilder.queryParam("circuit_key", circuitKey) }

                uriBuilder.build()
            }
            .retrieve()
            .bodyToFlow()
    }

    suspend fun getSessions(
        meetingKey: Int? = null,
        sessionKey: Int? = null,
        year: Int? = null
    ): Flow<OpenF1SessionResponse> {
        if (meetingKey == null && year == null) {
            throw OpenF1ClientRequestException("One of meetingKey, sessionKey or year are required")
        }

        return webClient
            .get()
            .uri { uriBuilder ->
                uriBuilder.path("/sessions")
                addMeetingAndSessionKeyParams(uriBuilder, meetingKey, sessionKey)
                year?.also { year -> uriBuilder.queryParam("year", year) }

                uriBuilder.build()
            }
            .retrieve()
            .bodyToFlow()
    }

    suspend fun getResults(
        meetingKey: Int? = null,
        sessionKey: Int? = null
    ): Flow<OpenF1SessionResultResponse> {
        if (meetingKey == null && sessionKey == null) {
            throw OpenF1ClientRequestException("One of meetingKey or sessionKey are required")
        }

        return webClient
            .get()
            .uri { uriBuilder ->
                uriBuilder.path("/session_result")
                addMeetingAndSessionKeyParams(uriBuilder, meetingKey, sessionKey)

                uriBuilder.build()
            }
            .retrieve()
            .bodyToFlow()
    }

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
                addMeetingAndSessionKeyParams(uriBuilder, meetingKey, sessionKey)
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
                addMeetingAndSessionKeyParams(uriBuilder, meetingKey, sessionKey)
                driverNumber?.also { driverNumber -> uriBuilder.queryParam("driver_number", driverNumber) }
                compound?.also { compound -> uriBuilder.queryParam("compound", compound) }

                uriBuilder.build()
            }
            .retrieve()
            .bodyToFlow()
    }

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
                addMeetingAndSessionKeyParams(uriBuilder, meetingKey, sessionKey)
                driverNumber?.also { driverNumber -> uriBuilder.queryParam("driver_number", driverNumber) }

                uriBuilder.build()
            }
            .retrieve()
            .bodyToFlow()
    }

    private fun addMeetingAndSessionKeyParams(uriBuilder: UriBuilder, meetingKey: Int?, sessionKey: Int?) {
        meetingKey?.also { meetingKey -> uriBuilder.queryParam("meeting_key", meetingKey) }
        sessionKey?.also { sessionKey -> uriBuilder.queryParam("session_key", sessionKey) }
    }

    companion object {
        class OpenF1ClientRequestException(message: String) : RuntimeException(message)
    }
}