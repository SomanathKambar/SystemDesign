package com.systemdesign.infra.ratelimiter.strategy.fixedwindow

import com.systemdesign.infra.ratelimiter.core.TestClock
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FixedWindowBoundaryTest {

    @Test
    fun `demonstrate window boundary burst issue`() {
        val clock = TestClock(0)
        val limiter = FixedWindowRateLimiter(
            maxRequests = 10, 
            windowSizeMs = 1000, 
            clock = clock
        )

        // Advance to just before the end of the first window
        clock.advanceBy(999)
        
        // Burst of 10 requests at the end of the first window
        repeat(10) {
            assertTrue(limiter.allow("user1").allowed)
        }

        // Advance just into the next window
        clock.advanceBy(2) // Now at 1001ms
        
        // Another burst of 10 requests at the start of the second window
        repeat(10) {
            assertTrue(limiter.allow("user1").allowed, "Should allow in the new window")
        }
        
        // Total allowed: 20 requests in 2ms (from 999ms to 1001ms)
    }
}
