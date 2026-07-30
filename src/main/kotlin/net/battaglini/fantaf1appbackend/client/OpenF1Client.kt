package net.battaglini.fantaf1appbackend.client

import kotlinx.coroutines.flow.Flow
import net.battaglini.fantaf1appbackend.enums.openf1.OpenF1SessionName
import net.battaglini.fantaf1appbackend.enums.openf1.OpenF1TyreCompound
import net.battaglini.fantaf1appbackend.model.openf1.*

/**
 * Client for interacting with the OpenF1 API.
 *
 * This client provides methods to retrieve various F1 data such as drivers, races, sessions,
 * results, overtakes, stints, and laps.
 */
interface OpenF1Client {

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
    suspend fun getDrivers(
        sessionKeys: List<String> = emptyList(),
        meetingKey: Int? = null,
        acronym: String? = null,
        driverNumber: Int? = null
    ): Flow<OpenF1DriverResponse>

    /**
     * Retrieves a list of races (meetings) based on the provided criteria.
     *
     * @param meetingKey The meeting key to filter by.
     * @param year The year to filter by.
     * @param circuitKey The circuit key to filter by.
     * @return A [Flow] emitting [OpenF1MeetingResponse] objects.
     * @throws OpenF1ClientRequestException If none of the parameters are provided.
     */
    suspend fun getRaces(
        meetingKey: Int? = null,
        year: Int? = null,
        circuitKey: Int? = null
    ): Flow<OpenF1MeetingResponse>

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
    suspend fun getSessions(
        meetingKey: Int? = null,
        sessionKey: String? = null,
        sessionName: OpenF1SessionName? = null,
        year: Int? = null
    ): Flow<OpenF1SessionResponse>

    /**
     * Retrieves the results of a session.
     *
     * @param meetingKey The meeting key to filter by.
     * @param sessionKeys The session keys to filter by.
     * @return A [Flow] emitting [OpenF1SessionResultResponse] objects.
     * @throws OpenF1ClientRequestException If neither meetingKey nor sessionKey is provided.
     */
    suspend fun getResults(
        meetingKey: Int? = null,
        sessionKeys: List<String> = emptyList()
    ): Flow<OpenF1SessionResultResponse>

    suspend fun getQualifyingResults(
        meetingKey: Int? = null,
        sessionKeys: List<String> = emptyList()
    ): Flow<OpenF1QualifyingSessionResultResponse>

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
        sessionKey: String? = null,
        overtakingDriverNumber: Int? = null,
        overtakenDriverNumber: Int? = null
    ): Flow<OpenF1OvertakeResponse>

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
        sessionKey: String? = null,
        driverNumber: Int? = null,
        compound: OpenF1TyreCompound? = null
    ): Flow<OpenF1StintResponse>

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
        sessionKey: String? = null,
        driverNumber: Int? = null
    ): Flow<OpenF1LapResponse>

}
