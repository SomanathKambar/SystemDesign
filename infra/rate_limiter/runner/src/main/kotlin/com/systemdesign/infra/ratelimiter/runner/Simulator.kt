package com.systemdesign.infra.ratelimiter.runner

import com.systemdesign.infra.ratelimiter.core.EventEmittingRateLimiter
import com.systemdesign.infra.ratelimiter.core.TestClock
import com.systemdesign.infra.ratelimiter.core.event.RateLimitEvent
import java.util.*

class Simulator(
    private val limiter: EventEmittingRateLimiter,
    private val clock: TestClock
) {
    private val tickIntervalMs = 100L

    fun run(profile: TrafficProfile) {
        when (profile) {
            is TrafficProfile.Constant -> simulateConstant(profile)
            is TrafficProfile.Burst -> simulateBurst(profile)
            is TrafficProfile.Random -> simulateRandom(profile)
            is TrafficProfile.Boundary -> simulateBoundary(profile)
        }
    }

    private fun advanceTime(ms: Long) {
        var remaining = ms
        while (remaining >= tickIntervalMs) {
            clock.advanceBy(tickIntervalMs)
            limiter.emitTick()
            remaining -= tickIntervalMs
        }
        if (remaining > 0) {
            clock.advanceBy(remaining)
            limiter.emitTick()
        }
    }

    private fun simulateBoundary(profile: TrafficProfile.Boundary) {
        var elapsed = 0L
        while (elapsed < profile.durationMs) {
            // First burst at the very end of a window
            val windowEnd = ((elapsed / profile.windowSizeMs) + 1) * profile.windowSizeMs
            val justBeforeEnd = windowEnd - 10
            
            if (justBeforeEnd > elapsed) {
                advanceTime(justBeforeEnd - elapsed)
                elapsed = justBeforeEnd
            }

            repeat(profile.burstSize) {
                limiter.allow("sim-key")
            }

            // Second burst at the very start of the next window
            advanceTime(20)
            elapsed += 20

            repeat(profile.burstSize) {
                limiter.allow("sim-key")
            }

            // Advance to the next window boundary
            val nextWindowEnd = ((elapsed / profile.windowSizeMs) + 1) * profile.windowSizeMs
            if (nextWindowEnd > elapsed) {
                advanceTime(nextWindowEnd - elapsed)
                elapsed = nextWindowEnd
            }
        }
    }

    private fun simulateConstant(profile: TrafficProfile.Constant) {
        val intervalMs = (1000.0 / profile.requestsPerSecond).toLong()
        var elapsed = 0L
        while (elapsed < profile.durationMs) {
            limiter.allow("sim-key")
            advanceTime(intervalMs)
            elapsed += intervalMs
        }
    }

    private fun simulateBurst(profile: TrafficProfile.Burst) {
        var elapsed = 0L
        while (elapsed < profile.durationMs) {
            repeat(profile.burstSize) {
                limiter.allow("sim-key")
            }
            advanceTime(profile.intervalMs)
            elapsed += profile.intervalMs
        }
    }

    private fun simulateRandom(profile: TrafficProfile.Random) {
        val random = Random(42)
        var elapsed = 0L
        while (elapsed < profile.durationMs) {
            val requests = random.nextInt((profile.avgRequestsPerSecond * 2).toInt())
            repeat(requests) {
                limiter.allow("sim-key")
            }
            advanceTime(1000)
            elapsed += 1000
        }
    }
}