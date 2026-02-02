package net.battaglini.fantaf1appbackend.client

import kotlinx.coroutines.flow.Flow
import net.battaglini.fantaf1appbackend.configuration.OpenF1ApiProperties
import net.battaglini.fantaf1appbackend.model.openf1.OpenF1DriversResponse
import net.battaglini.fantaf1appbackend.model.openf1.OpenF1MeetingsResponse
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlow

@Component
class OpenF1Client(
    openF1ApiProperties: OpenF1ApiProperties
) {
    private val webClient: WebClient = WebClient.builder()
        .baseUrl("${openF1ApiProperties.baseUrl}/${openF1ApiProperties.apiVersion}")
        .build();

    suspend fun getDrivers(
        sessionKey: Int?,
        meetingKey: Int?,
        acronym: String?,
        driverNumber: Int?
    ): Flow<OpenF1DriversResponse> {
        return webClient
            .get()
            .uri { uriBuilder ->
                uriBuilder.path("/drivers")
                    .queryParam("session_key", sessionKey ?: "latest")
                    .queryParam("meeting_key", meetingKey ?: "latest")
                acronym?.let { acronym -> uriBuilder.queryParam("name_acronym", acronym) }
                driverNumber?.let { driverNumber -> uriBuilder.queryParam("driver_number", driverNumber) }

                return@uri uriBuilder.build()
            }
            .retrieve()
            .bodyToFlow()
    }

    suspend fun getRaces(
        meetingKey: Int?,
        year: Int?,
        circuitKey: Int?
    ): Flow<OpenF1MeetingsResponse> {
        if (meetingKey == null && year == null && circuitKey == null) {
            throw OpenF1ClientRequestException("One of meetingKey, year or circuitKey are required")
        }

        return webClient
            .get()
            .uri { uriBuilder ->
                uriBuilder.path("/meetings")
                meetingKey?.let { meetingKey -> uriBuilder.queryParam("meeting_key", meetingKey) }
                year?.let { year -> uriBuilder.queryParam("year", year) }
                circuitKey?.let { circuitKey -> uriBuilder.queryParam("circuit_key", circuitKey) }

                return@uri uriBuilder.build()
            }
            .retrieve()
            .bodyToFlow()
    }


    companion object {
        class OpenF1ClientRequestException(message: String) : RuntimeException(message)
    }
}