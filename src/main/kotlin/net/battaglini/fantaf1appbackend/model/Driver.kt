package net.battaglini.fantaf1appbackend.model

data class Driver(
    val driverId: String,
    val driverNumber: Int,
    val acronym: String,
    val driverAvatar: String,
    val initialCost: Int,
    val isActive: Boolean,
    val name: String,
    val teamName: String,
    val teamColour: String,
)
