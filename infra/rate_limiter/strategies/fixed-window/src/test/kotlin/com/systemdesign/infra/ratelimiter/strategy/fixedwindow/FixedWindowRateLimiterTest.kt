package com.systemdesign.infra.ratelimiter.strategy.fixedwindow

import com.systemdesign.infra.ratelimiter.core.TestClock
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FixedWindowRateLimiterTest {

    @Test
    fun `should allow requests within limit`() {
        val limiter = FixedWindowRateLimiter(maxRequests = 2, windowSizeMs = 1000)
        
        assertTrue(limiter.allow("user1").allowed)
        assertTrue(limiter.allow("user1").allowed)
    }

    @Test
    fun `should block requests exceeding limit`() {
        val limiter = FixedWindowRateLimiter(maxRequests = 1, windowSizeMs = 1000)
        
        assertTrue(limiter.allow("user2").allowed)
        
        val decision = limiter.allow("user2")
        assertFalse(decision.allowed)
        assertNotNull(decision.retryAfterMs)
    }

    @Test
    fun `should reset after window passes`() {
        val clock = TestClock(0)
        val limiter = FixedWindowRateLimiter(
            maxRequests = 1, 
            windowSizeMs = 1000, 
            clock = clock
        )

        // t=0: Allowed
        assertTrue(limiter.allow("user3").allowed)
        
        // t=0: Blocked
        assertFalse(limiter.allow("user3").allowed)

        // t=1001: New Window -> Allowed
        clock.advanceBy(1001)
        assertTrue(limiter.allow("user3").allowed)
    }
}