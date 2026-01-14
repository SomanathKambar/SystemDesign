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
        val state = store.get(key) ?: TokenBucketState(capacity, now)

        // 1. Calculate refill
        val timePassedMs = now - state.lastRefillTime
        val tokensToAdd = (timePassedMs * refillTokensPerSecond) / 1000.0
        val refilledTokens = min(capacity, state.tokens + tokensToAdd)

        // 2. Decide
        return if (refilledTokens >= 1.0) {
            val newState = TokenBucketState(refilledTokens - 1.0, now)
            store.save(key, newState)
            Decision(allowed = true)
        } else {
            // Calculate when the next token will be available
            val needed = 1.0 - refilledTokens
            val waitTimeMs = (needed * 1000.0 / refillTokensPerSecond).toLong()
            Decision(
                allowed = false,
                retryAfterMs = waitTimeMs
            )
        }
    }
}
