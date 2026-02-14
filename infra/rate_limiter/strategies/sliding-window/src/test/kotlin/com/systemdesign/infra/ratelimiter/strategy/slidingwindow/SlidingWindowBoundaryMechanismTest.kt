package com.systemdesign.infra.ratelimiter.strategy.slidingwindow

import com.systemdesign.infra.ratelimiter.core.TestClock
import com.systemdesign.infra.ratelimiter.core.model.RequestContext
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SlidingWindowBoundaryMechanismTest {

    @Test
    fun `should mitigate window boundary burst issue`() {
        val clock = TestClock(0)
        val limiter = SlidingWindowMechanism(
            maxRequests = 10, 
            windowSizeMs = 1000, 
            clock = clock
        )

        val context = RequestContext(key = "user1")

        // Advance to 999ms
        clock.advanceBy(999)
        
        // Burst of 10 requests at the end of the first window
        repeat(10) {
            assertTrue(limiter.execute(context).allowed)
        }

        // Advance to 1001ms
        clock.advanceBy(2)
        
        // At 1001ms, weight is 0.999. Estimated count = 0 + 10 * 0.999 = 9.99
        // Projected count = 9.99 + 1 = 10.99 > 10. 
        // So the 1st request in the new window should be BLOCKED (unlike fixed window).
        assertFalse(limiter.execute(context).allowed, "Should block immediately after window boundary if prev was full")
        
        // Advance to 1100ms
        // Weight = 1.0 - (100/1000) = 0.9.
        // Estimated = 0 + 10 * 0.9 = 9.0.
        // Projected = 9.0 + 1 = 10.0 <= 10. ALLOWED.
        clock.setTime(1100)
        assertTrue(limiter.execute(context).allowed)
    }
}
