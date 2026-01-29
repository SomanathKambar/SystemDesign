package com.systemdesign.infra.ratelimiter.strategy.tokenbucket

import com.systemdesign.infra.ratelimiter.core.RateLimiter
import com.systemdesign.infra.ratelimiter.core.TokenBucketStore
import com.systemdesign.infra.ratelimiter.core.model.Decision
import com.systemdesign.infra.ratelimiter.core.model.TokenBucketState
import java.time.Clock
import kotlin.math.min

class TokenBucketRateLimiter(
    private val capacity: Double,
    private val refillTokensPerSecond: Double,
    private val store: TokenBucketStore = InMemoryTokenBucketStore(),
    private val clock: Clock = Clock.systemUTC()
) : RateLimiter {

    override fun allow(key: String): Decision {
        val now = clock.millis()
        var decision: Decision? = null

        store.compute(key) { state ->
            val currentState = state ?: TokenBucketState(capacity, now)

            // 1. Calculate refill
            val timePassedMs = now - currentState.lastRefillTime
            val tokensToAdd = (timePassedMs * refillTokensPerSecond) / 1000.0
            val refilledTokens = min(capacity, currentState.tokens + tokensToAdd)

            // 2. Decide
            if (refilledTokens >= 1.0) {
                decision = Decision(allowed = true)
                TokenBucketState(refilledTokens - 1.0, now)
            } else {
                val needed = 1.0 - refilledTokens
                val waitTimeMs = (needed * 1000.0 / refillTokensPerSecond).toLong()
                decision = Decision(
                    allowed = false,
                    retryAfterMs = waitTimeMs
                )
                // Update state to reflect partial refill, or keep as is?
                // Updating is better to avoid recalculating large time diffs repeatedly
                TokenBucketState(refilledTokens, now)
            }
        }

        return decision!!
    }
}
