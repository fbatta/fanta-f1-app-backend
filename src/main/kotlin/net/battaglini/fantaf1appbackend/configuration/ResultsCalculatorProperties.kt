package net.battaglini.fantaf1appbackend.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "results-calculator")
data class ResultsCalculatorProperties(
    val enable: Boolean = false,
    val dryRun: Boolean = false,
)
