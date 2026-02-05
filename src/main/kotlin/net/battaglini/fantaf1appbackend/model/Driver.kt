package net.battaglini.fantaf1appbackend.model

data class Driver(
    val driverId: String,
    val driverNumber: Int,
    val acronym: String,
    val initialCost: Int,
    val currentCost: Int,
    val isActive: Boolean,
    val name: String
)
