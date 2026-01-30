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
    val strategy by option(help = "Strategy to use").default("TOKEN_BUCKET")
    val capacity by option(help = "Capacity/Limit").double().default(10.0)
    val rate by option(help = "Refill Rate / Window Limit").double().default(2.0)
    val duration by option(help = "Duration in MS").long().default(10000)
    val name by option(help = "Experiment Name").default("Default Experiment")

    override fun run() {
        val clock = TestClock(0)
        val capturedEvents = mutableListOf<RateLimitEvent>()
        val consumer: (RateLimitEvent) -> Unit = { capturedEvents.add(it) }

        val limiter = when (strategy.uppercase()) {
            "TOKEN_BUCKET" -> TokenBucketRateLimiter(
                capacity = capacity,
                refillTokensPerSecond = rate,
                clock = clock
            )
            "FIXED_WINDOW" -> FixedWindowRateLimiter(
                windowSizeMs = 1000,
                maxRequests = capacity.toInt(),
                clock = clock
            )
            "SLIDING_WINDOW_COUNTER" -> SlidingWindowRateLimiter(
                windowSizeMs = 1000,
                maxRequests = capacity.toInt(),
                clock = clock
            )
            "SLIDING_WINDOW_LOG" -> SlidingWindowLogRateLimiter(
                windowSizeMs = 1000,
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
        
        // Define profile
        val profile = TrafficProfile.Burst("Burst Traffic", 5, 500, duration)
        
        echo("Running $strategy experiment: $name...")
        simulator.run(profile)

        val metadata = ExperimentMetadata(
            id = UUID.randomUUID().toString().take(8),
            name = name,
            description = "Simulation of $strategy with capacity $capacity and rate $rate",
            strategy = strategy.uppercase(),
            config = mapOf(
                "capacity" to capacity.toString(),
                "rate" to rate.toString(),
                "duration" to duration.toString()
            ),
            profile = profile,
            timestamp = System.currentTimeMillis()
        )

        val writer = ExperimentWriter()
        val dir = writer.write(metadata, capturedEvents)
        
        echo("Experiment saved to: ${dir.absolutePath}")
        echo("Total events: ${capturedEvents.size}")
    }
}

fun main(args: Array<String>) = RunExperiment().main(args)