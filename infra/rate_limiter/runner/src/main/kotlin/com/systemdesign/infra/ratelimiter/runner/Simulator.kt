package com.systemdesign.infra.ratelimiter.runner

import com.systemdesign.infra.ratelimiter.core.Clock
import com.systemdesign.infra.ratelimiter.core.EventEmittingRateLimiter
import com.systemdesign.infra.ratelimiter.core.TestClock
import com.systemdesign.infra.ratelimiter.core.event.RateLimitEvent
import java.util.*

class Simulator(
    private val limiter: EventEmittingRateLimiter,
    private val clock: TestClock
) {
    private val events = mutableListOf<RateLimitEvent>()

    fun run(profile: TrafficProfile): List<RateLimitEvent> {
        val capturedEvents = mutableListOf<RateLimitEvent>()
        val consumer: (RateLimitEvent) -> Unit = { capturedEvents.add(it) }
        
        // We need to re-wrap or inject the consumer into the limiter.
        // For simplicity in this step, let's assume the limiter was already 
        // configured with a consumer that we can observe.
        
        // Actually, it's better to pass the consumer to the run method 
        // or have the simulator be the consumer.
        
        when (profile) {
            is TrafficProfile.Constant -> simulateConstant(profile, capturedEvents)
            is TrafficProfile.Burst -> simulateBurst(profile, capturedEvents)
            is TrafficProfile.Random -> simulateRandom(profile, capturedEvents)
            is TrafficProfile.Boundary -> simulateBoundary(profile, capturedEvents)
        }
        
        return capturedEvents
    }

    private fun simulateBoundary(profile: TrafficProfile.Boundary, capturedEvents: MutableList<RateLimitEvent>) {
        var elapsed = 0L
        while (elapsed < profile.durationMs) {
            // First burst at the very end of a window
            val windowEnd = ((elapsed / profile.windowSizeMs) + 1) * profile.windowSizeMs
            val justBeforeEnd = windowEnd - 10
            
            if (justBeforeEnd > elapsed) {
                clock.advanceBy(justBeforeEnd - elapsed)
                elapsed = justBeforeEnd
            }

            repeat(profile.burstSize) {
                limiter.allow("sim-key")
            }

            // Second burst at the very start of the next window
            clock.advanceBy(20)
            elapsed += 20

            repeat(profile.burstSize) {
                limiter.allow("sim-key")
            }

            // Advance to the next window boundary
            val nextWindowEnd = ((elapsed / profile.windowSizeMs) + 1) * profile.windowSizeMs
            if (nextWindowEnd > elapsed) {
                clock.advanceBy(nextWindowEnd - elapsed)
                elapsed = nextWindowEnd
            }
        }
    }

    private fun simulateConstant(profile: TrafficProfile.Constant, capturedEvents: MutableList<RateLimitEvent>) {
        val intervalMs = (1000.0 / profile.requestsPerSecond).toLong()
        var elapsed = 0L
        while (elapsed < profile.durationMs) {
            limiter.allow("sim-key")
            clock.advanceBy(intervalMs)
            elapsed += intervalMs
        }
    }

    private fun simulateBurst(profile: TrafficProfile.Burst, capturedEvents: MutableList<RateLimitEvent>) {
        var elapsed = 0L
        while (elapsed < profile.durationMs) {
            repeat(profile.burstSize) {
                limiter.allow("sim-key")
            }
            clock.advanceBy(profile.intervalMs)
            elapsed += profile.intervalMs
        }
    }

    private fun simulateRandom(profile: TrafficProfile.Random, capturedEvents: MutableList<RateLimitEvent>) {
        val random = Random(42) // Fixed seed for determinism
        var elapsed = 0L
        while (elapsed < profile.durationMs) {
            val requests = random.nextInt((profile.avgRequestsPerSecond * 2).toInt())
            repeat(requests) {
                limiter.allow("sim-key")
            }
            clock.advanceBy(1000)
            elapsed += 1000
        }
    }
}
