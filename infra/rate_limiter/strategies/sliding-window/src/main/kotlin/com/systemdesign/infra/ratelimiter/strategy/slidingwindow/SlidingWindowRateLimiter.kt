package com.systemdesign.infra.ratelimiter.strategy.slidingwindow

import com.systemdesign.infra.ratelimiter.core.RateLimiter
import com.systemdesign.infra.ratelimiter.core.StateStore
import com.systemdesign.infra.ratelimiter.core.model.CounterState
import com.systemdesign.infra.ratelimiter.core.model.Decision
import java.time.Clock

/**
 * Sliding Window Counter implementation.
 * It approximates the count by taking a weighted average of the current and previous windows.
 */
class SlidingWindowRateLimiter(
    private val limit: Int,
    private val windowSizeMs: Long,
    private val stateStore: StateStore,
    private val clock: Clock = Clock.systemUTC()
) : RateLimiter {

    override fun allow(key: String): Decision {
        val now = clock.millis()
        val currentWindowStart = (now / windowSizeMs) * windowSizeMs
        val previousWindowStart = currentWindowStart - windowSizeMs

        val currentWindowKey = "$key:$currentWindowStart"
        val previousWindowKey = "$key:$previousWindowStart"

        val currentCount = stateStore.get(currentWindowKey)?.count ?: 0
        val previousCount = stateStore.get(previousWindowKey)?.count ?: 0

        val timeElapsedInCurrentWindow = now - currentWindowStart
        val weight = 1.0 - (timeElapsedInCurrentWindow.toDouble() / windowSizeMs)
        
        val estimatedCount = currentCount + (previousCount * weight)

        if (estimatedCount >= limit) {
            // For sliding window, the retry time is harder to calculate exactly as it's a smooth curve,
            // but we can estimate when the count would drop below the limit.
            // Simplified: try again after a small fraction of the window or when this specific window ends.
            val resetTime = currentWindowStart + windowSizeMs
            return Decision(
                allowed = false,
                retryAfterMs = resetTime - now,
                context = mapOf("estimatedCount" to estimatedCount)
            )
        }

        // Increment current window
        stateStore.save(
            key = currentWindowKey,
            state = CounterState(currentCount + 1, currentWindowStart),
            ttlMs = windowSizeMs * 2 // Keep long enough for next window to use as 'previous'
        )

        return Decision(
            allowed = true,
            context = mapOf("estimatedCount" to estimatedCount + 1)
        )
    }
}