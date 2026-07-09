package net.battaglini.fantaf1appbackend.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "open-f1")
data class OpenF1ApiProperties(
    val baseUrl: String,
    val apiVersion: String,
    val rateLimit: RateLimit = RateLimit()
) {
    data class RateLimit(
        val maxRatePerSecond: Int = 3,
        val maxRatePerMinute: Int = 30,
        val maxBurstPerMinute: Int = 30
    )
}