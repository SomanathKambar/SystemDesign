package com.systemdesign.infra.ratelimiter.strategy.fixedwindow

import com.systemdesign.infra.ratelimiter.core.model.CounterState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class FixedWindowRateLimiterTest {

    @Test
    fun `should allow requests within limit`() {
        val limiter = FixedWindowRateLimiter(limit = 2, windowSizeMs = 1000)
        
        assertTrue(limiter.allow("user1").allowed)
        assertTrue(limiter.allow("user1").allowed)
    }

    @Test
    fun `should block requests exceeding limit`() {
        val limiter = FixedWindowRateLimiter(limit = 1, windowSizeMs = 1000)
        
        assertTrue(limiter.allow("user2").allowed)
        
        val decision = limiter.allow("user2")
        assertFalse(decision.allowed)
        assertNotNull(decision.retryAfterMs)
    }

    @Test
    fun `should reset after window passes`() {
        // Mock clock starting at 0
        var currentTime = 0L
        val mockClock = object : Clock() {
            override fun getZone(): ZoneId = ZoneId.of("UTC")
            override fun withZone(zone: ZoneId?): Clock = this
            override fun instant(): Instant = Instant.ofEpochMilli(currentTime)
        }

        val limiter = FixedWindowRateLimiter(
            limit = 1, 
            windowSizeMs = 1000, 
            clock = mockClock
        )

        // t=0: Allowed
        assertTrue(limiter.allow("user3").allowed)
        
        // t=0: Blocked
        assertFalse(limiter.allow("user3").allowed)

        // t=1001: New Window -> Allowed
        currentTime = 1001
        assertTrue(limiter.allow("user3").allowed)
    }
}
