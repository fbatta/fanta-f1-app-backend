package net.battaglini.fantaf1appbackend.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "results-calculator")
data class ResultsCalculatorProperties(
    /**
     * Controls whether result calculator is enabled or not
     */
    val enable: Boolean = false,
    /**
     * Run calculator in dry-run mode, i.e. don't store results in the database
     */
    val dryRun: Boolean = false,
)
