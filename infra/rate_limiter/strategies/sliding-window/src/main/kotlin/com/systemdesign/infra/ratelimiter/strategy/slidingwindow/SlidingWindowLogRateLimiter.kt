package com.systemdesign.infra.ratelimiter.strategy.slidingwindow

import com.systemdesign.infra.ratelimiter.core.Clock
import com.systemdesign.infra.ratelimiter.core.RateLimiter
import com.systemdesign.infra.ratelimiter.core.SlidingWindowStore
import com.systemdesign.infra.ratelimiter.core.SystemClock
import com.systemdesign.infra.ratelimiter.core.model.Decision
import com.systemdesign.infra.ratelimiter.core.model.SlidingWindowLog

class SlidingWindowLogRateLimiter(
    private val windowSizeMs: Long,
    private val maxRequests: Int,
    private val store: SlidingWindowStore = InMemorySlidingWindowStore(),
    private val clock: Clock = SystemClock()
) : RateLimiter {

    override fun allow(key: String): Decision {
        val now = clock.currentTimeMillis()
        val windowStart = now - windowSizeMs

        var decision: Decision? = null

        store.compute(key, windowSizeMs) { log ->
            val currentTimestamps = log?.timestamps?.filter { it >= windowStart } ?: emptyList()
            
            if (currentTimestamps.size < maxRequests) {
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