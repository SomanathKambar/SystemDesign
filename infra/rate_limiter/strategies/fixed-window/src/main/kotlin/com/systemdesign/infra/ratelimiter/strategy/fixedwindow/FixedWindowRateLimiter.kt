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

        var decision: Decision? = null

        stateStore.compute(windowKey, windowSizeMs) { state ->
            if (state != null && state.count >= limit) {
                val resetTime = windowStart + windowSizeMs
                decision = Decision(
                    allowed = false,
                    retryAfterMs = resetTime - now
                )
                state
            } else {
                val newCount = (state?.count ?: 0) + 1
                decision = Decision(allowed = true)
                CounterState(newCount, windowStart)
            }
        }

        return decision!!
    }
}
