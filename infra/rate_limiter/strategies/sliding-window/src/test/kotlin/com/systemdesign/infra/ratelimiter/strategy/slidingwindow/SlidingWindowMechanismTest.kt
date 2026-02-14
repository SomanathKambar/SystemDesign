package com.systemdesign.infra.ratelimiter.strategy.slidingwindow

import com.systemdesign.infra.ratelimiter.core.TestClock
import com.systemdesign.infra.ratelimiter.core.model.RequestContext
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SlidingWindowMechanismTest {

    @Test
    fun `should allow requests within limit`() {
        val limiter = SlidingWindowMechanism(maxRequests = 3, windowSizeMs = 1000)
        
        val context = RequestContext(key = "u1")
        assertTrue(limiter.execute(context).allowed)
        assertTrue(limiter.execute(context).allowed)
        assertTrue(limiter.execute(context).allowed)
        assertFalse(limiter.execute(context).allowed)
    }

    @Test
    fun `should slide window accurately`() {
        val clock = TestClock(1000)
        val limiter = SlidingWindowMechanism(
            maxRequests = 2, 
            windowSizeMs = 1000, 
            clock = clock
        )
        
        val context = RequestContext(key = "u2")

        // T=1000
        assertTrue(limiter.execute(context).allowed) // Current: 1, Prev: 0
        
        // T=1100
        clock.setTime(1100)
        assertTrue(limiter.execute(context).allowed) // Current: 2, Prev: 0
        
        // T=1200 -> Blocked
        clock.setTime(1200)
        assertFalse(limiter.execute(context).allowed)

        // T=2001 -> Current window [2000, 3000). Previous [1000, 2000).
        // At T=2001, weight for prev is 1 - (1/1000) = 0.999
        // Estimated = 0 + 2 * 0.999 = 1.998.
        // Projected = 1.998 + 1 = 2.998 > 2. BLOCKED.
        clock.setTime(2001)
        assertFalse(limiter.execute(context).allowed)
        
        // T=2500 -> Weight = 0.5.
        // Estimated = 0 + 2 * 0.5 = 1.0.
        // Projected = 1.0 + 1 = 2.0 <= 2. ALLOWED.
        clock.setTime(2500)
        assertTrue(limiter.execute(context).allowed) // Current: 1, Prev: 2
        
        // T=3001 -> Current [3000, 4000), Prev [2000, 3000). Prev count was 1.
        // Weight is approx 1.0.
        // Estimated = 0 + 1 * 0.999 = 0.999.
        // Projected = 0.999 + 1 = 1.999 <= 2. ALLOWED.
        clock.setTime(3001)
        assertTrue(limiter.execute(context).allowed)
    }
}