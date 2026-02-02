package net.battaglini.fantaf1appbackend.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "open-f1")
data class OpenF1ApiProperties(
    val baseUrl: String,
    val apiVersion: String
)