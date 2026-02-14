package com.systemdesign.infra.ratelimiter.strategy.tokenbucket

import com.systemdesign.infra.ratelimiter.core.Clock
import com.systemdesign.infra.ratelimiter.core.Mechanism
import com.systemdesign.infra.ratelimiter.core.SystemClock
import com.systemdesign.infra.ratelimiter.core.TokenBucketStore
import com.systemdesign.infra.ratelimiter.core.model.Decision
import com.systemdesign.infra.ratelimiter.core.model.RequestContext
import com.systemdesign.infra.ratelimiter.core.model.TokenBucketState
import kotlin.math.min

class TokenBucketMechanism(
    private val capacity: Double,
    private val refillTokensPerSecond: Double,
    private val store: TokenBucketStore = InMemoryTokenBucketStore(),
    private val clock: Clock = SystemClock()
) : Mechanism {

    override fun execute(context: RequestContext): Decision {
        val key = context.key
        val cost = context.tokens.toDouble()
        val now = clock.currentTimeMillis()
        var decision: Decision? = null

        store.compute(key, 3600_000L) { state ->
            val currentState = state ?: TokenBucketState(capacity, now)

            // 1. Calculate refill
            val timePassedMs = now - currentState.lastRefillTime
            val tokensToAdd = (timePassedMs * refillTokensPerSecond) / 1000.0
            val refilledTokens = min(capacity, currentState.tokens + tokensToAdd)

            val meta = mapOf(
                "tokensBefore" to currentState.tokens,
                "tokensAdded" to tokensToAdd,
                "tokensAfterRefill" to refilledTokens,
                "cost" to cost
            )

            // 2. Decide
            if (refilledTokens >= cost) {
                decision = Decision(
                    allowed = true,
                    reason = "Allowed",
                    metadata = meta + ("tokensAfterConsuming" to (refilledTokens - cost))
                )
                TokenBucketState(refilledTokens - cost, now)
            } else {
                val needed = cost - refilledTokens
                val waitTimeMs = (needed * 1000.0 / refillTokensPerSecond).toLong()
                decision = Decision(
                    allowed = false,
                    reason = "Not enough tokens. Available: $refilledTokens, Required: $cost",
                    retryAfterMs = waitTimeMs,
                    metadata = meta
                )
                TokenBucketState(refilledTokens, now)
            }
        }

        return decision!!
    }
}
