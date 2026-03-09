package net.battaglini.fantaf1appbackend.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "seeding")
data class SeedingProperties(
    val drivers: Boolean = false,
    val raceWeekends: Boolean = false
)