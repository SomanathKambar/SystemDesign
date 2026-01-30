package com.systemdesign.infra.ratelimiter.strategy.leakybucket

import com.systemdesign.infra.ratelimiter.core.TestClock
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LeakyBucketRateLimiterTest {

    @Test
    fun `should allow requests within capacity`() {
        val clock = TestClock(1000)
        val limiter = LeakyBucketRateLimiter(capacity = 5.0, leakRatePerSecond = 1.0, clock = clock)

        repeat(5) {
            assertTrue(limiter.allow("user1").allowed)
        }
        
        // 6th request should be blocked
        assertFalse(limiter.allow("user1").allowed)
    }

    @Test
    fun `should leak over time`() {
        val clock = TestClock(1000)
        val limiter = LeakyBucketRateLimiter(capacity = 5.0, leakRatePerSecond = 1.0, clock = clock)

        repeat(5) {
            limiter.allow("user1")
        }
        assertFalse(limiter.allow("user1").allowed)

        // Wait 1 second, 1 token should leak
        clock.advanceBy(1000)
        assertTrue(limiter.allow("user1").allowed)
        assertFalse(limiter.allow("user1").allowed)
    }

    @Test
    fun `should calculate retry after correctly`() {
        val clock = TestClock(1000)
        val limiter = LeakyBucketRateLimiter(capacity = 5.0, leakRatePerSecond = 1.0, clock = clock)

        repeat(5) {
            limiter.allow("user1")
        }
        
        val decision = limiter.allow("user1")
        assertFalse(decision.allowed)
        assertEquals(1000L, decision.retryAfterMs)
        
        clock.advanceBy(500)
        val decision2 = limiter.allow("user1")
        assertEquals(500L, decision2.retryAfterMs)
    }
}
