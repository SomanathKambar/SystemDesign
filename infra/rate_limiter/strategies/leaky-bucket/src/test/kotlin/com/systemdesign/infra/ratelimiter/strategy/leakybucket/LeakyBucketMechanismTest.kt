package com.systemdesign.infra.ratelimiter.strategy.leakybucket

import com.systemdesign.infra.ratelimiter.core.TestClock
import com.systemdesign.infra.ratelimiter.core.model.RequestContext
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LeakyBucketMechanismTest {

    @Test
    fun `should allow requests within capacity`() {
        val clock = TestClock(1000)
        val limiter = LeakyBucketMechanism(capacity = 5.0, leakRatePerSecond = 1.0, clock = clock)
        val context = RequestContext(key = "user1")

        repeat(5) {
            assertTrue(limiter.execute(context).allowed)
        }
        
        // 6th request should be blocked
        assertFalse(limiter.execute(context).allowed)
    }

    @Test
    fun `should leak over time`() {
        val clock = TestClock(1000)
        val limiter = LeakyBucketMechanism(capacity = 5.0, leakRatePerSecond = 1.0, clock = clock)
        val context = RequestContext(key = "user1")

        repeat(5) {
            limiter.execute(context)
        }
        assertFalse(limiter.execute(context).allowed)

        // Wait 1 second, 1 token should leak
        clock.advanceBy(1000)
        assertTrue(limiter.execute(context).allowed)
        assertFalse(limiter.execute(context).allowed)
    }

    @Test
    fun `should calculate retry after correctly`() {
        val clock = TestClock(1000)
        val limiter = LeakyBucketMechanism(capacity = 5.0, leakRatePerSecond = 1.0, clock = clock)
        val context = RequestContext(key = "user1")

        repeat(5) {
            limiter.execute(context)
        }
        
        val decision = limiter.execute(context)
        assertFalse(decision.allowed)
        // With current water 5.0, need to add 1.0, capacity 5.0. 
        // Need to leak 1.0 unit. Leak rate 1.0/sec. Wait 1000ms.
        assertEquals(1000L, decision.retryAfterMs)
        
        clock.advanceBy(500)
        // Water is 4.5. +1.0 = 5.5. Excess 0.5. Wait 500ms.
        val decision2 = limiter.execute(context)
        assertEquals(500L, decision2.retryAfterMs)
    }
}