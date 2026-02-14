package com.systemdesign.infra.ratelimiter.strategy.fixedwindow

import com.systemdesign.infra.ratelimiter.core.TestClock
import com.systemdesign.infra.ratelimiter.core.model.RequestContext
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FixedWindowMechanismTest {

    @Test
    fun `should allow requests within limit`() {
        val limiter = FixedWindowMechanism(maxRequests = 2, windowSizeMs = 1000)
        
        val context = RequestContext(key = "user1")
        assertTrue(limiter.execute(context).allowed)
        assertTrue(limiter.execute(context).allowed)
    }

    @Test
    fun `should block requests exceeding limit`() {
        val limiter = FixedWindowMechanism(maxRequests = 1, windowSizeMs = 1000)
        
        val context = RequestContext(key = "user2")
        assertTrue(limiter.execute(context).allowed)
        
        val decision = limiter.execute(context)
        assertFalse(decision.allowed)
        assertNotNull(decision.retryAfterMs)
        assertEquals("Fixed window limit exceeded. Max: 1, Current: 1, Requested: 1", decision.reason)
    }

    @Test
    fun `should reset after window passes`() {
        val clock = TestClock(0)
        val limiter = FixedWindowMechanism(
            maxRequests = 1, 
            windowSizeMs = 1000, 
            clock = clock
        )

        val context = RequestContext(key = "user3")

        // t=0: Allowed
        assertTrue(limiter.execute(context).allowed)
        
        // t=0: Blocked
        assertFalse(limiter.execute(context).allowed)

        // t=1001: New Window -> Allowed
        clock.advanceBy(1001)
        assertTrue(limiter.execute(context).allowed)
    }
}
