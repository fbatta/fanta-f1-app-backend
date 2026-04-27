package net.battaglini.fantaf1appbackend.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "pricing")
data class PricingProperties(
    val enable: Boolean = true,
    val dryRun: Boolean = false,
    val rollingWindowSize: Int = 3,
    val driverWeight: Double = 0.8,
    val teamWeight: Double = 0.2,
    val priceFloor: Double = 20.0,
    val priceCeiling: Double = 85.0,
    val targetAvgPrice: Double = 50.0,
    val maxAvgPriceThreshold: Double = 52.0
)
