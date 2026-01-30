package com.systemdesign.infra.ratelimiter.strategy.fixedwindow

import com.systemdesign.infra.ratelimiter.core.Clock
import com.systemdesign.infra.ratelimiter.core.RateLimiter
import com.systemdesign.infra.ratelimiter.core.StateStore
import com.systemdesign.infra.ratelimiter.core.SystemClock
import com.systemdesign.infra.ratelimiter.core.model.Decision
import com.systemdesign.infra.ratelimiter.core.model.FixedWindowCounter

class FixedWindowRateLimiter(
    private val windowSizeMs: Long,
    private val maxRequests: Int,
    private val store: StateStore<FixedWindowCounter> = InMemoryStateStore(),
    private val clock: Clock = SystemClock()
) : RateLimiter {

    override fun allow(key: String): Decision {
        val now = clock.currentTimeMillis()
        val windowStart = (now / windowSizeMs) * windowSizeMs

        var decision: Decision? = null

        // TTL should be at least one full window to ensure we can look back if needed,
        // although for fixed window, we only care about the current window.
        store.compute(key, windowSizeMs * 2) { state: FixedWindowCounter? ->
            val isNewWindow = state == null || state.windowStart != windowStart
            val currentState = if (isNewWindow) {
                FixedWindowCounter(0, windowStart)
            } else {
                state!!
            }

            val context = mapOf(
                "windowStart" to windowStart,
                "isNewWindow" to isNewWindow,
                "currentCount" to currentState.count
            )

            if (currentState.count >= maxRequests) {
                val resetTime = windowStart + windowSizeMs
                decision = Decision(
                    allowed = false,
                    retryAfterMs = resetTime - now,
                    context = context
                )
                currentState
            } else {
                decision = Decision(
                    allowed = true,
                    context = context + ("newCount" to (currentState.count + 1))
                )
                FixedWindowCounter(currentState.count + 1, windowStart)
            }
        }

        return decision!!
    }
}