package com.systemdesign.infra.ratelimiter.strategy.leakybucket

import com.systemdesign.infra.ratelimiter.core.Clock
import com.systemdesign.infra.ratelimiter.core.LeakyBucketStore
import com.systemdesign.infra.ratelimiter.core.Mechanism
import com.systemdesign.infra.ratelimiter.core.SystemClock
import com.systemdesign.infra.ratelimiter.core.model.Decision
import com.systemdesign.infra.ratelimiter.core.model.LeakyBucketState
import com.systemdesign.infra.ratelimiter.core.model.RequestContext
import kotlin.math.max

class LeakyBucketMechanism(
    private val capacity: Double,
    private val leakRatePerSecond: Double,
    private val store: LeakyBucketStore = InMemoryLeakyBucketStore(),
    private val clock: Clock = SystemClock()
) : Mechanism {

    override fun execute(context: RequestContext): Decision {
        val key = context.key
        val cost = context.tokens.toDouble()
        val now = clock.currentTimeMillis()
        var decision: Decision? = null

        store.compute(key, 3600_000L) { state ->
            val currentState = state ?: LeakyBucketState(0.0, now)

            // 1. Calculate leak
            val timePassedMs = now - currentState.lastLeakTime
            val leakedAmount = (timePassedMs * leakRatePerSecond) / 1000.0
            val currentWaterLevel = max(0.0, currentState.waterLevel - leakedAmount)

            val meta = mapOf(
                "waterBefore" to currentState.waterLevel,
                "leakedAmount" to leakedAmount,
                "waterAfterLeak" to currentWaterLevel,
                "cost" to cost
            )

            // 2. Decide
            if (currentWaterLevel + cost <= capacity) {
                val newWaterLevel = currentWaterLevel + cost
                decision = Decision(
                    allowed = true,
                    reason = "Allowed",
                    metadata = meta + ("newWaterLevel" to newWaterLevel)
                )
                LeakyBucketState(newWaterLevel, now)
            } else {
                val neededLeak = currentWaterLevel + cost - capacity
                val waitTimeMs = (neededLeak * 1000.0 / leakRatePerSecond).toLong()
                
                decision = Decision(
                    allowed = false,
                    reason = "Leaky bucket overflow",
                    retryAfterMs = waitTimeMs,
                    metadata = meta
                )
                LeakyBucketState(currentWaterLevel, now)
            }
        }

        return decision!!
    }
}