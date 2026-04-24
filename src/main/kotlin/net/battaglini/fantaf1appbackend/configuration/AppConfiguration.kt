package net.battaglini.fantaf1appbackend.configuration

import kotlinx.datetime.TimeZone
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import kotlin.time.Clock

@Configuration
class AppConfiguration {
    @Bean
    fun clock(): Clock = Clock.System

    @Bean
    fun timeZone(appProperties: AppProperties): TimeZone =
        appProperties.timeZone?.let { TimeZone.of(it) } ?: TimeZone.currentSystemDefault()
}