package net.battaglini.fantaf1appbackend.client

import com.neutrine.krate.rateLimiter
import kotlinx.coroutines.delay
import net.battaglini.fantaf1appbackend.configuration.OpenF1ApiProperties
import org.springframework.stereotype.Component
import java.time.temporal.ChronoUnit

@Component
class OpenF1RateLimiter(
    openF1ApiProperties: OpenF1ApiProperties
) {
    private val perSecondBucket = rateLimiter(maxRate = openF1ApiProperties.rateLimit.maxRatePerSecond.toLong())
    private val perMinuteBucket = rateLimiter(maxRate = openF1ApiProperties.rateLimit.maxBurstPerMinute.toLong()) {
        maxBurst = openF1ApiProperties.rateLimit.maxBurstPerMinute.toLong()
        maxRateTimeUnit = ChronoUnit.MINUTES
    }

    suspend fun acquire() {
        // Per-minute bucket (0.5 tokens/sec) is more restrictive than per-second bucket (3 tokens/sec)
        // Acquire from the more restrictive bucket first
        while (!perMinuteBucket.tryTake()) {
            delay(50)
        }
        while (!perSecondBucket.tryTake()) {
            delay(50)
        }
    }
}
