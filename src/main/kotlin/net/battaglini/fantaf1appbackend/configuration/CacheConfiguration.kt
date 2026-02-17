package net.battaglini.fantaf1appbackend.configuration

import com.github.benmanes.caffeine.cache.AsyncCache
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
    fun meetingSessionsCache(): AsyncCache<Any, Any> = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(10))
        .buildAsync()

    @Bean
    fun meetingsCache(): AsyncCache<Any, Any> = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(60))
        .buildAsync()

    @Bean
    fun driversCache(): AsyncCache<Any, Any> = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(60))
        .buildAsync()

    @Bean
    fun cacheManager(
        meetingSessionsCache: AsyncCache<Any, Any>,
        meetingsCache: AsyncCache<Any, Any>,
        driversCache: AsyncCache<Any, Any>
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
        caffeineCacheManager.setAsyncCacheMode(true)
        return caffeineCacheManager
    }

    companion object {
        const val MEETING_SESSIONS_CACHE = "meetingSessionsCache"
        const val MEETINGS_CACHE_NAME = "meetingsCache"
        const val DRIVERS_CACHE = "driversCache"
    }
}