package com.systemdesign.infra.ratelimiter.strategy.tokenbucket

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class TokenBucketRateLimiterTest {

    @Test
    fun `should allow burst up to capacity`() {
        val limiter = TokenBucketRateLimiter(capacity = 3.0, refillTokensPerSecond = 1.0)
        
        assertTrue(limiter.allow("user1").allowed)
        assertTrue(limiter.allow("user1").allowed)
        assertTrue(limiter.allow("user1").allowed)
        assertFalse(limiter.allow("user1").allowed)
    }

    @Test
    fun `should refill over time`() {
        var currentTime = 1000L
        val mockClock = object : Clock() {
            override fun getZone(): ZoneId = ZoneId.of("UTC")
            override fun withZone(zone: ZoneId?): Clock = this
            override fun instant(): Instant = Instant.ofEpochMilli(currentTime)
        }

        // Capacity 1, refill 1 token per second
        val limiter = TokenBucketRateLimiter(
            capacity = 1.0, 
            refillTokensPerSecond = 1.0, 
            clock = mockClock
        )

        // T=1000: Consume only token
        assertTrue(limiter.allow("user2").allowed)
        assertFalse(limiter.allow("user2").allowed)

        // T=1500: Half token refilled (total 0.5), still not enough for 1.0
        currentTime = 1500
        assertFalse(limiter.allow("user2").allowed)

        // T=2000: Full token refilled (total 1.0)
        currentTime = 2000
        assertTrue(limiter.allow("user2").allowed)
    }

    @Test
    fun `should respect capacity limit during refill`() {
        var currentTime = 1000L
        val mockClock = object : Clock() {
            override fun getZone(): ZoneId = ZoneId.of("UTC")
            override fun withZone(zone: ZoneId?): Clock = this
            override fun instant(): Instant = Instant.ofEpochMilli(currentTime)
        }

        val limiter = TokenBucketRateLimiter(
            capacity = 2.0, 
            refillTokensPerSecond = 10.0, 
            clock = mockClock
        )

        // T=1000: Full bucket (2.0)
        // Wait 10 seconds -> refilling 100 tokens, but should cap at 2.0
        currentTime = 11000
        
        assertTrue(limiter.allow("user3").allowed)
        assertTrue(limiter.allow("user3").allowed)
        assertFalse(limiter.allow("user3").allowed) // Only 2 allowed because of cap
    }
}
