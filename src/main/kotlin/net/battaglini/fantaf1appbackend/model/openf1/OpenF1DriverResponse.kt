package net.battaglini.fantaf1appbackend.model.openf1

import com.fasterxml.jackson.annotation.JsonProperty
import net.battaglini.fantaf1appbackend.model.Driver

data class OpenF1DriverResponse(
    @JsonProperty("broadcast_name")
    val broadcastName: String,
    @JsonProperty("driver_number")
    val driverNumber: Int,
    @JsonProperty("first_name")
    val firstName: String,
    @JsonProperty("full_name")
    val fullName: String,
    @JsonProperty("headshot_url")
    val headshotUrl: String,
    @JsonProperty("last_name")
    val lastName: String,
    @JsonProperty("meeting_key")
    val meetingKey: String,
    @JsonProperty("name_acronym")
    val nameAcronym: String,
    @JsonProperty("session_key")
    val sessionKey: String,
    @JsonProperty("team_colour")
    val teamColour: String,
    @JsonProperty("team_name")
    val teamName: String
) {
    companion object {
        fun OpenF1DriverResponse.toDriver(driverId: String) = Driver(
            driverId = driverId,
            driverNumber = driverNumber,
            acronym = nameAcronym,
            initialCost = 0,
            isActive = true,
            name = fullName
        )
    }
}
