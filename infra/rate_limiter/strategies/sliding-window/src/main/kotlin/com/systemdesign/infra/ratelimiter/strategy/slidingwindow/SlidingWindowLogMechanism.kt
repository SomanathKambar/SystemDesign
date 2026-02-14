package com.systemdesign.infra.ratelimiter.strategy.slidingwindow

import com.systemdesign.infra.ratelimiter.core.Clock
import com.systemdesign.infra.ratelimiter.core.Mechanism
import com.systemdesign.infra.ratelimiter.core.SlidingWindowStore
import com.systemdesign.infra.ratelimiter.core.SystemClock
import com.systemdesign.infra.ratelimiter.core.model.Decision
import com.systemdesign.infra.ratelimiter.core.model.RequestContext
import com.systemdesign.infra.ratelimiter.core.model.SlidingWindowLog

class SlidingWindowLogMechanism(
    private val windowSizeMs: Long,
    private val maxRequests: Int,
    private val store: SlidingWindowStore = InMemorySlidingWindowStore(),
    private val clock: Clock = SystemClock()
) : Mechanism {

    override fun execute(context: RequestContext): Decision {
        val key = context.key
        val cost = context.tokens.toInt()
        val now = clock.currentTimeMillis()
        val windowStart = now - windowSizeMs

        var decision: Decision? = null

        store.compute(key, windowSizeMs) { log ->
            val currentTimestamps = log?.timestamps?.filter { it >= windowStart } ?: emptyList()
            
            if (currentTimestamps.size + cost <= maxRequests) {
                // Add 'cost' entries of 'now' to represent weight
                val newEntries = List(cost) { now }
                val newTimestamps = currentTimestamps + newEntries
                
                decision = Decision(
                    allowed = true,
                    reason = "Allowed",
                    metadata = mapOf("count" to newTimestamps.size)
                )
                SlidingWindowLog(newTimestamps)
            } else {
                val oldestTimestamp = if (currentTimestamps.isNotEmpty()) currentTimestamps.first() else now
                val retryAfterMs = oldestTimestamp + windowSizeMs - now
                decision = Decision(
                    allowed = false,
                    reason = "Sliding window log limit exceeded",
                    retryAfterMs = retryAfterMs,
                    metadata = mapOf("count" to currentTimestamps.size, "cost" to cost)
                )
                SlidingWindowLog(currentTimestamps)
            }
        }

        return decision!!
    }
}
