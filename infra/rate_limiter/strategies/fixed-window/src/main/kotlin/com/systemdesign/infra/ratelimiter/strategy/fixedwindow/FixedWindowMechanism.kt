package com.systemdesign.infra.ratelimiter.strategy.fixedwindow

import com.systemdesign.infra.ratelimiter.core.Clock
import com.systemdesign.infra.ratelimiter.core.Mechanism
import com.systemdesign.infra.ratelimiter.core.StateStore
import com.systemdesign.infra.ratelimiter.core.SystemClock
import com.systemdesign.infra.ratelimiter.core.model.Decision
import com.systemdesign.infra.ratelimiter.core.model.FixedWindowCounter
import com.systemdesign.infra.ratelimiter.core.model.RequestContext

class FixedWindowMechanism(
    private val windowSizeMs: Long,
    private val maxRequests: Int,
    private val store: StateStore<FixedWindowCounter> = InMemoryStateStore(),
    private val clock: Clock = SystemClock()
) : Mechanism {

    override fun execute(context: RequestContext): Decision {
        val key = context.key
        val now = clock.currentTimeMillis()
        val windowStart = (now / windowSizeMs) * windowSizeMs

        var decision: Decision? = null

        // TTL should be at least one full window
        store.compute(key, windowSizeMs * 2) { state: FixedWindowCounter? ->
            val isNewWindow = state == null || state.windowStart != windowStart
            val currentState = if (isNewWindow) {
                FixedWindowCounter(0, windowStart)
            } else {
                state!!
            }

            val cost = context.tokens.toInt()
            val projectedCount = currentState.count + cost

            val meta = mapOf(
                "windowStart" to windowStart,
                "currentCount" to currentState.count,
                "cost" to cost
            )

            if (projectedCount > maxRequests) {
                val resetTime = windowStart + windowSizeMs
                decision = Decision(
                    allowed = false,
                    reason = "Fixed window limit exceeded. Max: $maxRequests, Current: ${currentState.count}, Requested: $cost",
                    retryAfterMs = resetTime - now,
                    metadata = meta
                )
                currentState
            } else {
                decision = Decision(
                    allowed = true,
                    reason = "Allowed",
                    metadata = meta + ("newCount" to projectedCount)
                )
                FixedWindowCounter(projectedCount, windowStart)
            }
        }

        return decision!!
    }
}
