package net.battaglini.fantaf1appbackend.configuration

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
@EnableCaching
class CacheConfiguration {
    @Bean
    fun meetingSessionsCache(): Cache<Any, Any> = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(10))
        .build()

    @Bean
    fun meetingsCache(): Cache<Any, Any> = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(60))
        .build()

    @Bean
    fun driversCache(): Cache<Any, Any> = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(60))
        .build()

    @Bean
    fun cacheManager(
        meetingSessionsCache: Cache<Any, Any>,
        meetingsCache: Cache<Any, Any>,
        driversCache: Cache<Any, Any>
    ): CacheManager {
        val caffeineCacheManager = CaffeineCacheManager()
        caffeineCacheManager.registerCustomCache(
            MEETING_SESSIONS_CACHE,
            meetingSessionsCache
        )
        caffeineCacheManager.registerCustomCache(
            MEETINGS_CACHE_NAME,
            meetingsCache
        )
        caffeineCacheManager.registerCustomCache(
            DRIVERS_CACHE,
            driversCache
        )
        return caffeineCacheManager
    }

    companion object {
        const val MEETING_SESSIONS_CACHE = "meetingSessionsCache"
        const val MEETINGS_CACHE_NAME = "meetingsCache"
        const val DRIVERS_CACHE = "driversCache"
    }
}