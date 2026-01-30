package com.systemdesign.infra.ratelimiter.runner

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.double
import com.github.ajalt.clikt.parameters.types.long
import com.systemdesign.infra.ratelimiter.core.EventEmittingRateLimiter
import com.systemdesign.infra.ratelimiter.core.TestClock
import com.systemdesign.infra.ratelimiter.core.event.RateLimitEvent
import com.systemdesign.infra.ratelimiter.strategy.fixedwindow.FixedWindowRateLimiter
import com.systemdesign.infra.ratelimiter.strategy.tokenbucket.TokenBucketRateLimiter
import com.systemdesign.infra.ratelimiter.strategy.slidingwindow.SlidingWindowRateLimiter
import com.systemdesign.infra.ratelimiter.strategy.slidingwindow.SlidingWindowLogRateLimiter
import java.util.*

class RunExperiment : CliktCommand() {
    override fun run() {
        val strategies = listOf("TOKEN_BUCKET", "FIXED_WINDOW", "SLIDING_WINDOW_COUNTER", "SLIDING_WINDOW_LOG")
        val durations = 30000L // 30 seconds for better visualization
        
        strategies.forEach { strategy ->
            runScenario(strategy, "Burst", TrafficProfile.Burst("Burst Traffic", 8, 2000, durations))
            runScenario(strategy, "HighLoad", TrafficProfile.Constant("High Load", 5.0, durations))
            runScenario(strategy, "Boundary", TrafficProfile.Boundary("Boundary Burst", 5000, 10, durations))
        }
    }

    private fun runScenario(strategy: String, scenarioName: String, profile: TrafficProfile) {
        val clock = TestClock(0)
        val capturedEvents = mutableListOf<RateLimitEvent>()
        val consumer: (RateLimitEvent) -> Unit = { capturedEvents.add(it) }

        val capacity = 10.0
        val rate = 2.0
        val windowSizeMs = 5000L

        val limiter = when (strategy.uppercase()) {
            "TOKEN_BUCKET" -> TokenBucketRateLimiter(
                capacity = capacity,
                refillTokensPerSecond = rate,
                clock = clock
            )
            "FIXED_WINDOW" -> FixedWindowRateLimiter(
                windowSizeMs = windowSizeMs,
                maxRequests = capacity.toInt(),
                clock = clock
            )
            "SLIDING_WINDOW_COUNTER" -> SlidingWindowRateLimiter(
                windowSizeMs = windowSizeMs,
                maxRequests = capacity.toInt(),
                clock = clock
            )
            "SLIDING_WINDOW_LOG" -> SlidingWindowLogRateLimiter(
                windowSizeMs = windowSizeMs,
                maxRequests = capacity.toInt(),
                clock = clock
            )
            else -> throw IllegalArgumentException("Unknown strategy: $strategy")
        }

        val emitter = EventEmittingRateLimiter(
            delegate = limiter,
            strategyName = strategy,
            nodeId = "node-1",
            clock = clock,
            eventConsumer = consumer
        )

        val simulator = Simulator(emitter, clock)
        
        echo("Running $strategy scenario: $scenarioName...")
        simulator.run(profile)

        val metadata = ExperimentMetadata(
            id = UUID.randomUUID().toString().take(8),
            name = "$strategy - $scenarioName",
            description = "Simulation of $strategy under $scenarioName traffic.",
            strategy = strategy.uppercase(),
            config = mapOf(
                "capacity" to capacity.toString(),
                "rate" to rate.toString(),
                "duration" to profile.let { 
                    when(it) {
                        is TrafficProfile.Constant -> it.durationMs
                        is TrafficProfile.Burst -> it.durationMs
                        is TrafficProfile.Random -> it.durationMs
                        is TrafficProfile.Boundary -> it.durationMs
                    }
                }.toString(),
                "windowSizeMs" to windowSizeMs.toString()
            ),
            profile = profile,
            timestamp = System.currentTimeMillis()
        )

        val writer = ExperimentWriter()
        val dir = writer.write(metadata, capturedEvents)
        
        echo("Experiment saved to: ${dir.absolutePath}")
    }
}

fun main(args: Array<String>) = RunExperiment().main(args)