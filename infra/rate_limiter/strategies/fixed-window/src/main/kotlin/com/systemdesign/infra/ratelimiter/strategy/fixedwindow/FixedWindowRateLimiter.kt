package com.systemdesign.infra.ratelimiter.strategy.fixedwindow

import com.systemdesign.infra.ratelimiter.core.RateLimiter
import com.systemdesign.infra.ratelimiter.core.StateStore
import com.systemdesign.infra.ratelimiter.core.model.CounterState
import com.systemdesign.infra.ratelimiter.core.model.Decision
import java.time.Clock

class FixedWindowRateLimiter(
    private val limit: Int,
    private val windowSizeMs: Long,
    private val stateStore: StateStore = InMemoryStateStore(),
    private val clock: Clock = Clock.systemUTC()
) : RateLimiter {

    override fun allow(key: String): Decision {
        val now = clock.millis()
        val windowStart = (now / windowSizeMs) * windowSizeMs
        val windowKey = "$key:$windowStart"

        val state = stateStore.get(windowKey)
        
        // If current window exists and is full
        if (state != null && state.count >= limit) {
             val resetTime = windowStart + windowSizeMs
             return Decision(
                 allowed = false,
                 retryAfterMs = resetTime - now
             )
        }

        // Increment or create
        val newCount = (state?.count ?: 0) + 1
        stateStore.save(
            key = windowKey,
            state = CounterState(newCount, windowStart),
            ttlMs = windowSizeMs // In Redis this would auto-expire old keys
        )

        return Decision(allowed = true)
    }
}
