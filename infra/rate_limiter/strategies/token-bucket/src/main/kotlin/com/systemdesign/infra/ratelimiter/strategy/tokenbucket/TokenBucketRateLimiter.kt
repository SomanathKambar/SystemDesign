package com.systemdesign.infra.ratelimiter.strategy.tokenbucket

import com.systemdesign.infra.ratelimiter.core.Clock
import com.systemdesign.infra.ratelimiter.core.RateLimiter
import com.systemdesign.infra.ratelimiter.core.SystemClock
import com.systemdesign.infra.ratelimiter.core.TokenBucketStore
import com.systemdesign.infra.ratelimiter.core.model.Decision
import com.systemdesign.infra.ratelimiter.core.model.TokenBucketState
import kotlin.math.min

class TokenBucketRateLimiter(
    private val capacity: Double,
    private val refillTokensPerSecond: Double,
    private val store: TokenBucketStore = InMemoryTokenBucketStore(),
    private val clock: Clock = SystemClock()
) : RateLimiter {

    override fun allow(key: String): Decision {
        val now = clock.currentTimeMillis()
        var decision: Decision? = null

        // TTL for token bucket can be long-lived, let's use 1 hour for now.
        store.compute(key, 3600_000L) { state ->
            val currentState = state ?: TokenBucketState(capacity, now)

            // 1. Calculate refill
            val timePassedMs = now - currentState.lastRefillTime
            val tokensToAdd = (timePassedMs * refillTokensPerSecond) / 1000.0
            val refilledTokens = min(capacity, currentState.tokens + tokensToAdd)

            val context = mutableMapOf<String, Any>(
                "tokensBefore" to currentState.tokens,
                "tokensAdded" to tokensToAdd,
                "tokensAfterRefill" to refilledTokens
            )

            // 2. Decide
            if (refilledTokens >= 1.0) {
                decision = Decision(
                    allowed = true,
                    context = context + ("tokensAfterConsuming" to (refilledTokens - 1.0))
                )
                TokenBucketState(refilledTokens - 1.0, now)
            } else {
                val needed = 1.0 - refilledTokens
                val waitTimeMs = (needed * 1000.0 / refillTokensPerSecond).toLong()
                decision = Decision(
                    allowed = false,
                    retryAfterMs = waitTimeMs,
                    context = context
                )
                TokenBucketState(refilledTokens, now)
            }
        }

        return decision!!
    }
}