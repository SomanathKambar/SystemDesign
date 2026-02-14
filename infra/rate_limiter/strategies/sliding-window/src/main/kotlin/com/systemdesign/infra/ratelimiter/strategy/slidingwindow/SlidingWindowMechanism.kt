package com.systemdesign.infra.ratelimiter.strategy.slidingwindow

import com.systemdesign.infra.ratelimiter.core.Clock
import com.systemdesign.infra.ratelimiter.core.Mechanism
import com.systemdesign.infra.ratelimiter.core.StateStore
import com.systemdesign.infra.ratelimiter.core.SystemClock
import com.systemdesign.infra.ratelimiter.core.model.Decision
import com.systemdesign.infra.ratelimiter.core.model.RequestContext
import com.systemdesign.infra.ratelimiter.core.model.SlidingWindowCounter

class SlidingWindowMechanism(
    private val windowSizeMs: Long,
    private val maxRequests: Int,
    private val store: StateStore<SlidingWindowCounter> = InMemoryStateStore(),
    private val clock: Clock = SystemClock()
) : Mechanism {

    override fun execute(context: RequestContext): Decision {
        val key = context.key
        val cost = context.tokens.toInt()
        val now = clock.currentTimeMillis()
        val currentWindowStart = (now / windowSizeMs) * windowSizeMs
        val previousWindowStart = currentWindowStart - windowSizeMs

        var decision: Decision? = null
        
        // TTL is 2 windows to keep both current and previous state
        store.compute(key, windowSizeMs * 2) { state: SlidingWindowCounter? ->
            val isNewWindow = state == null || state.windowStart != currentWindowStart
            val currentState = if (isNewWindow) {
                val prevCount = if (state != null && state.windowStart == previousWindowStart) state.count else 0
                SlidingWindowCounter(count = 0, windowStart = currentWindowStart, previousCount = prevCount)
            } else {
                state!!
            }

            val timeElapsedInCurrentWindow = now - currentWindowStart
            val weight = 1.0 - (timeElapsedInCurrentWindow.toDouble() / windowSizeMs)
            val estimatedCount = currentState.count + (currentState.previousCount * weight)
            val projectedCount = estimatedCount + cost

            val meta = mapOf(
                "windowStart" to currentWindowStart,
                "isNewWindow" to isNewWindow,
                "estimatedCount" to estimatedCount,
                "projectedCount" to projectedCount,
                "cost" to cost
            )

            if (projectedCount > maxRequests) {
                val resetTime = currentWindowStart + windowSizeMs
                decision = Decision(
                    allowed = false,
                    reason = "Sliding window limit exceeded. Max: $maxRequests, Est: $projectedCount",
                    retryAfterMs = resetTime - now,
                    metadata = meta
                )
                currentState
            } else {
                decision = Decision(
                    allowed = true,
                    reason = "Allowed",
                    metadata = meta + ("newCount" to (currentState.count + cost))
                )
                currentState.copy(count = currentState.count + cost)
            }
        }

        return decision!!
    }
}
