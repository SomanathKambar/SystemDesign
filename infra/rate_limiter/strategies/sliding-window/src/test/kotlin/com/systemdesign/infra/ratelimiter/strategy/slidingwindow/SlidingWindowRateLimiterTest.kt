package com.systemdesign.infra.ratelimiter.strategy.slidingwindow

import com.systemdesign.infra.ratelimiter.core.TestClock
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SlidingWindowRateLimiterTest {

    @Test
    fun `should allow requests within limit`() {
        val limiter = SlidingWindowRateLimiter(maxRequests = 3, windowSizeMs = 1000)
        
        assertTrue(limiter.allow("u1").allowed)
        assertTrue(limiter.allow("u1").allowed)
        assertTrue(limiter.allow("u1").allowed)
        assertFalse(limiter.allow("u1").allowed)
    }

    @Test
    fun `should slide window accurately`() {
        val clock = TestClock(1000)
        val limiter = SlidingWindowRateLimiter(
            maxRequests = 2, 
            windowSizeMs = 1000, 
            clock = clock
        )

        // T=1000
        assertTrue(limiter.allow("u2").allowed) // Current: 1, Prev: 0
        
        // T=1100
        clock.setTime(1100)
        assertTrue(limiter.allow("u2").allowed) // Current: 2, Prev: 0
        
        // T=1200 -> Blocked
        clock.setTime(1200)
        assertFalse(limiter.allow("u2").allowed)

        // T=2001 -> Current window [2000, 3000). Previous [1000, 2000).
        // At T=2001, weight for prev is 1 - (1/1000) = 0.999
        // Estimated = 0 + 2 * 0.999 = 1.998 < 2. ALLOWED.
        clock.setTime(2001)
        assertTrue(limiter.allow("u2").allowed) // Current: 1, Prev: 2
        
        // After allowing, Current = 1.
        // T=2002 -> Estimated = 1 + 2 * (1 - 2/1000) = 1 + 1.996 = 2.996 >= 2. BLOCKED.
        clock.setTime(2002)
        assertFalse(limiter.allow("u2").allowed)
        
        // T=3001 -> Current [3000, 4000), Prev [2000, 3000). Prev count was 1.
        // Estimated = 0 + 1 * weight < 2. ALLOWED.
        clock.setTime(3001)
        assertTrue(limiter.allow("u2").allowed)
    }
}