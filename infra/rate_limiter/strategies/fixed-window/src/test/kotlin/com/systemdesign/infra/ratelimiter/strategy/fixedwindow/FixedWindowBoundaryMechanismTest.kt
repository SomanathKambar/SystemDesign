package com.systemdesign.infra.ratelimiter.strategy.fixedwindow

import com.systemdesign.infra.ratelimiter.core.TestClock
import com.systemdesign.infra.ratelimiter.core.model.RequestContext
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FixedWindowBoundaryMechanismTest {

    @Test
    fun `should show window boundary burst issue`() {
        val clock = TestClock(0)
        val limiter = FixedWindowMechanism(
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

        // Advance to 1001ms (New window)
        clock.advanceBy(2)
        
        // Another burst of 10 requests allowed immediately!
        // This is the classic fixed window boundary problem (20 requests in 3ms)
        repeat(10) {
            assertTrue(limiter.execute(context).allowed)
        }
        
        // 21st request blocked
        assertFalse(limiter.execute(context).allowed)
    }
}