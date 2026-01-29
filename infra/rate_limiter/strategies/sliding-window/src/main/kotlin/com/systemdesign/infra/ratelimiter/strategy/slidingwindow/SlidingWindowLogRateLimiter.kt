package com.systemdesign.infra.ratelimiter.strategy.slidingwindow

import com.systemdesign.infra.ratelimiter.core.RateLimiter
import com.systemdesign.infra.ratelimiter.core.SlidingWindowStore
import com.systemdesign.infra.ratelimiter.core.model.Decision
import com.systemdesign.infra.ratelimiter.core.model.SlidingWindowLog
import java.time.Clock

class SlidingWindowLogRateLimiter(
    private val limit: Int,
    private val windowSizeMs: Long,
    private val store: SlidingWindowStore,
    private val clock: Clock = Clock.systemUTC()
) : RateLimiter {

    override fun allow(key: String): Decision {
        val now = clock.millis()
        val windowStart = now - windowSizeMs

        var decision: Decision? = null

        store.compute(key, windowSizeMs) { log ->
            val currentTimestamps = log?.timestamps?.filter { it >= windowStart } ?: emptyList()
            
            if (currentTimestamps.size < limit) {
                val newTimestamps = currentTimestamps + now
                decision = Decision(allowed = true)
                SlidingWindowLog(newTimestamps)
            } else {
                val oldestTimestamp = currentTimestamps.first()
                val retryAfterMs = oldestTimestamp + windowSizeMs - now
                decision = Decision(
                    allowed = false,
                    retryAfterMs = retryAfterMs
                )
                SlidingWindowLog(currentTimestamps)
            }
        }

        return decision!!
    }
}
