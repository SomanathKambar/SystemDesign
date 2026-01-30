package com.systemdesign.infra.ratelimiter.strategy.leakybucket

import com.systemdesign.infra.ratelimiter.core.Clock
import com.systemdesign.infra.ratelimiter.core.LeakyBucketStore
import com.systemdesign.infra.ratelimiter.core.RateLimiter
import com.systemdesign.infra.ratelimiter.core.SystemClock
import com.systemdesign.infra.ratelimiter.core.model.Decision
import com.systemdesign.infra.ratelimiter.core.model.LeakyBucketState
import kotlin.math.max

class LeakyBucketRateLimiter(
    private val capacity: Double,
    private val leakRatePerSecond: Double,
    private val store: LeakyBucketStore = InMemoryLeakyBucketStore(),
    private val clock: Clock = SystemClock()
) : RateLimiter {

    override fun allow(key: String): Decision {
        val now = clock.currentTimeMillis()
        var decision: Decision? = null

        store.compute(key, 3600_000L) { state ->
            val currentState = state ?: LeakyBucketState(0.0, now)

            // 1. Calculate leak
            val timePassedMs = now - currentState.lastLeakTime
            val leakedAmount = (timePassedMs * leakRatePerSecond) / 1000.0
            val currentWaterLevel = max(0.0, currentState.waterLevel - leakedAmount)

            val context = mutableMapOf<String, Any>(
                "waterBefore" to currentState.waterLevel,
                "leakedAmount" to leakedAmount,
                "waterAfterLeak" to currentWaterLevel
            )

            // 2. Decide
            if (currentWaterLevel + 1.0 <= capacity) {
                val newWaterLevel = currentWaterLevel + 1.0
                decision = Decision(
                    allowed = true,
                    context = context + ("newWaterLevel" to newWaterLevel)
                )
                LeakyBucketState(newWaterLevel, now)
            } else {
                // How long until we have space for 1.0 units?
                // currentWaterLevel + 1.0 - leak = capacity
                // leak = currentWaterLevel + 1.0 - capacity
                val neededLeak = currentWaterLevel + 1.0 - capacity
                val waitTimeMs = (neededLeak * 1000.0 / leakRatePerSecond).toLong()
                
                decision = Decision(
                    allowed = false,
                    retryAfterMs = waitTimeMs,
                    context = context
                )
                LeakyBucketState(currentWaterLevel, now)
            }
        }

        return decision!!
    }
}
