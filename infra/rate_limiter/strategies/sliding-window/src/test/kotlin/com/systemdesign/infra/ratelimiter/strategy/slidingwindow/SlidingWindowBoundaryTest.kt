package com.systemdesign.infra.ratelimiter.strategy.slidingwindow

import com.systemdesign.infra.ratelimiter.core.TestClock
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SlidingWindowBoundaryTest {

    @Test
    fun `should mitigate window boundary burst issue`() {
        val clock = TestClock(0)
        val limiter = SlidingWindowRateLimiter(
            maxRequests = 10, 
            windowSizeMs = 1000, 
            clock = clock
        )

        // Advance to 999ms
        clock.advanceBy(999)
        
        // Burst of 10 requests at the end of the first window
        repeat(10) {
            assertTrue(limiter.allow("user1").allowed)
        }

        // Advance to 1001ms
        clock.advanceBy(2)
        
        // At 1001ms, weight is 0.999. Estimated count = 0 + 10 * 0.999 = 9.99
        // So the 1st request in the new window MIGHT be allowed if it's just below the limit.
        assertTrue(limiter.allow("user1").allowed, "Might allow one due to weighting")
        
        // But the 2nd request should definitely be blocked
        // Estimated count = 1 + 10 * 0.999 = 10.99
        assertFalse(limiter.allow("user1").allowed, "Should block subsequent requests")
    }
}
