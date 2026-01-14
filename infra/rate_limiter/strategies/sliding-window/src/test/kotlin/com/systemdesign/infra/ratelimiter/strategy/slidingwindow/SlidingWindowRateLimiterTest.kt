package com.systemdesign.infra.ratelimiter.strategy.slidingwindow

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class SlidingWindowRateLimiterTest {

    @Test
    fun `should allow requests within limit`() {
        val limiter = SlidingWindowRateLimiter(limit = 3, windowSizeMs = 1000)
        
        assertTrue(limiter.allow("u1").allowed)
        assertTrue(limiter.allow("u1").allowed)
        assertTrue(limiter.allow("u1").allowed)
        assertFalse(limiter.allow("u1").allowed)
    }

    @Test
    fun `should slide window accurately`() {
        var currentTime = 1000L
        val mockClock = object : Clock() {
            override fun getZone(): ZoneId = ZoneId.of("UTC")
            override fun withZone(zone: ZoneId?): Clock = this
            override fun instant(): Instant = Instant.ofEpochMilli(currentTime)
        }

        val limiter = SlidingWindowRateLimiter(
            limit = 2, 
            windowSizeMs = 1000, 
            clock = mockClock
        )

        // T=1000
        assertTrue(limiter.allow("u2").allowed)
        
        // T=1100
        currentTime = 1100
        assertTrue(limiter.allow("u2").allowed)
        
        // T=1200 -> Blocked
        currentTime = 1200
        assertFalse(limiter.allow("u2").allowed)

        // T=2001 -> T=1000 is expired, T=1100 remains. One slot available.
        currentTime = 2001
        val decision = limiter.allow("u2")
        assertTrue(decision.allowed)
        
        // T=2002 -> Still 2 in window (1100, 2001). Blocked.
        currentTime = 2002
        assertFalse(limiter.allow("u2").allowed)
        
        // T=2101 -> T=1100 expired. One slot available.
        currentTime = 2101
        assertTrue(limiter.allow("u2").allowed)
    }
}
