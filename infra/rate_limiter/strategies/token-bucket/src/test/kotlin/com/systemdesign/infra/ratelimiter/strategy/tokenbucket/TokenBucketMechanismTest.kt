package com.systemdesign.infra.ratelimiter.strategy.tokenbucket

import com.systemdesign.infra.ratelimiter.core.TestClock
import com.systemdesign.infra.ratelimiter.core.model.RequestContext
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TokenBucketMechanismTest {

    @Test
    fun `should allow burst up to capacity`() {
        val limiter = TokenBucketMechanism(capacity = 3.0, refillTokensPerSecond = 1.0)
        
        val context = RequestContext(key = "user1")
        assertTrue(limiter.execute(context).allowed)
        assertTrue(limiter.execute(context).allowed)
        assertTrue(limiter.execute(context).allowed)
        assertFalse(limiter.execute(context).allowed)
    }

    @Test
    fun `should refill over time`() {
        val clock = TestClock(1000)
        // Capacity 1, refill 1 token per second
        val limiter = TokenBucketMechanism(
            capacity = 1.0, 
            refillTokensPerSecond = 1.0, 
            clock = clock
        )

        val context = RequestContext(key = "user2")

        // T=1000: Consume only token
        assertTrue(limiter.execute(context).allowed)
        assertFalse(limiter.execute(context).allowed)

        // T=1500: Half token refilled (total 0.5), still not enough for 1.0
        clock.setTime(1500)
        assertFalse(limiter.execute(context).allowed)

        // T=2000: Full token refilled (total 1.0)
        clock.setTime(2000)
        assertTrue(limiter.execute(context).allowed)
    }

    @Test
    fun `should respect capacity limit during refill`() {
        val clock = TestClock(1000)
        val limiter = TokenBucketMechanism(
            capacity = 2.0, 
            refillTokensPerSecond = 10.0, 
            clock = clock
        )

        val context = RequestContext(key = "user3")

        // T=1000: Full bucket (2.0)
        // Wait 10 seconds -> refilling 100 tokens, but should cap at 2.0
        clock.setTime(11000)
        
        assertTrue(limiter.execute(context).allowed)
        assertTrue(limiter.execute(context).allowed)
        assertFalse(limiter.execute(context).allowed) // Only 2 allowed because of cap
    }
}
