package com.systemdesign.infra.ratelimiter.app

import com.systemdesign.infra.ratelimiter.core.RateLimiter
import com.systemdesign.infra.ratelimiter.strategy.fixedwindow.FixedWindowRateLimiter
import com.systemdesign.infra.ratelimiter.strategy.fixedwindow.InMemoryStateStore
import com.systemdesign.infra.ratelimiter.strategy.slidingwindow.SlidingWindowRateLimiter
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.util.concurrent.atomic.AtomicReference

@Serializable
enum class StrategyType {
    FIXED_WINDOW,
    SLIDING_WINDOW
}

@Serializable
data class ConfigRequest(
    val limit: Int, 
    val windowSizeMs: Long,
    val strategy: StrategyType = StrategyType.FIXED_WINDOW
)

@Serializable
data class RateLimitRequest(val key: String)

@Serializable
data class RateLimitResponse(
    val allowed: Boolean,
    val retryAfterMs: Long? = null,
    val currentLimit: Int,
    val currentWindowSizeMs: Long,
    val serverTimeMs: Long,
    val windowStartMs: Long,
    val strategy: StrategyType,
    val estimatedCount: Double? = null
)

@Serializable
data class ConfigUpdateResponse(
    val status: String,
    val config: ConfigRequest
)

@Serializable
data class ComparisonDataPoint(val second: Int, val fixedAllowed: Int, val slidingAllowed: Int)

@Serializable
data class ComparisonResult(
    val strategy: StrategyType,
    val allowed: Int,
    val blocked: Int,
    val throughput: Double,
    val description: String
)

@Serializable
data class ComparisonResponse(
    val results: List<ComparisonResult>,
    val winner: StrategyType,
    val logic: String,
    val timeSeries: List<ComparisonDataPoint>
)

class RateLimiterManager {
    private var currentConfig = ConfigRequest(5, 10000, StrategyType.FIXED_WINDOW)
    private val stateStore = InMemoryStateStore()
    
    private val limiterRef = AtomicReference<RateLimiter>(
        createLimiter(currentConfig)
    )

    private fun createLimiter(config: ConfigRequest): RateLimiter {
        return when (config.strategy) {
            StrategyType.FIXED_WINDOW -> FixedWindowRateLimiter(config.limit, config.windowSizeMs, stateStore)
            StrategyType.SLIDING_WINDOW -> SlidingWindowRateLimiter(config.limit, config.windowSizeMs, stateStore)
        }
    }

    fun runComparison(durationSec: Int, rps: Int, limit: Int, windowMs: Long): ComparisonResponse {
        val timeSeries = mutableListOf<ComparisonDataPoint>()
        
        // Use custom clocks to simulate time passing without Thread.sleep
        class ManualClock(var millis: Long) : java.time.Clock() {
            override fun getZone(): java.time.ZoneId = java.time.ZoneOffset.UTC
            override fun withZone(zone: java.time.ZoneId?): java.time.Clock = this
            override fun instant(): java.time.Instant = java.time.Instant.ofEpochMilli(millis)
            override fun millis(): Long = millis
        }

        val startTime = System.currentTimeMillis()
        val fixedClock = ManualClock(startTime)
        val slidingClock = ManualClock(startTime)

        val fixed = FixedWindowRateLimiter(limit, windowMs, InMemoryStateStore(), fixedClock)
        val sliding = SlidingWindowRateLimiter(limit, windowMs, InMemoryStateStore(), slidingClock)
        
        var totalFixedAllowed = 0
        var totalSlidingAllowed = 0
        
        for (sec in 1..durationSec) {
            var secFixedAllowed = 0
            var secSlidingAllowed = 0
            
            for (r in 1..rps) {
                // Spread requests within the second
                val offset = (r * (1000 / rps)).toLong()
                fixedClock.millis = startTime + (sec - 1) * 1000 + offset
                slidingClock.millis = startTime + (sec - 1) * 1000 + offset
                
                if (fixed.allow("comp").allowed) secFixedAllowed++
                if (sliding.allow("comp").allowed) secSlidingAllowed++
            }
            
            totalFixedAllowed += secFixedAllowed
            totalSlidingAllowed += secSlidingAllowed
            timeSeries.add(ComparisonDataPoint(sec, secFixedAllowed, secSlidingAllowed))
        }

        val results = listOf(
            ComparisonResult(
                StrategyType.FIXED_WINDOW, 
                totalFixedAllowed, 
                (durationSec * rps) - totalFixedAllowed,
                (totalFixedAllowed.toDouble() / durationSec),
                "Predictable but prone to boundary bursts (Step-function behavior)."
            ),
            ComparisonResult(
                StrategyType.SLIDING_WINDOW, 
                totalSlidingAllowed, 
                (durationSec * rps) - totalSlidingAllowed,
                (totalSlidingAllowed.toDouble() / durationSec),
                "Smoother distribution, prevents edge-case spikes using weighted averages."
            )
        )

        // Winner logic: If throughput is tied, Sliding wins for smoothness.
        // If one has strictly better throughput, it wins.
        val winner = when {
            totalSlidingAllowed > totalFixedAllowed -> StrategyType.SLIDING_WINDOW
            totalFixedAllowed > totalSlidingAllowed -> StrategyType.FIXED_WINDOW
            else -> StrategyType.SLIDING_WINDOW // Default to sliding for better edge-case protection
        }
        
        return ComparisonResponse(
            results = results,
            winner = winner,
            logic = if (totalFixedAllowed == totalSlidingAllowed) 
                "Tied on throughput, but Sliding Window is preferred for preventing boundary bursts." 
                else "Winner determined by superior throughput under the specific load pattern.",
            timeSeries = timeSeries
        )
    }

    fun updateConfig(config: ConfigRequest) {
        this.currentConfig = config
        limiterRef.set(createLimiter(config))
    }

    fun allow(key: String): RateLimitResponse {
        val now = System.currentTimeMillis()
        val decision = limiterRef.get().allow(key)
        val windowStart = (now / currentConfig.windowSizeMs) * currentConfig.windowSizeMs
        
        return RateLimitResponse(
            allowed = decision.allowed,
            retryAfterMs = decision.retryAfterMs,
            currentLimit = currentConfig.limit,
            currentWindowSizeMs = currentConfig.windowSizeMs,
            serverTimeMs = now,
            windowStartMs = windowStart,
            strategy = currentConfig.strategy,
            estimatedCount = decision.context["estimatedCount"] as? Double ?: (decision.context["estimatedCount"] as? Int)?.toDouble()
        )
    }
    
    fun getConfig() = currentConfig
}

fun main() {
    embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) {
            json()
        }
        
        val manager = RateLimiterManager()

        routing {
            staticResources("/", "static", index = "index.html")

            route("/api") {
                get("/config") {
                    call.respond(manager.getConfig())
                }

                post("/config") {
                    val config = call.receive<ConfigRequest>()
                    manager.updateConfig(config)
                    call.respond(ConfigUpdateResponse(status = "updated", config = config))
                }

                post("/request") {
                    val req = call.receive<RateLimitRequest>()
                    val result = manager.allow(req.key)
                    call.respond(result)
                }

                get("/compare") {
                    val duration = call.request.queryParameters["duration"]?.toInt() ?: 10
                    val rps = call.request.queryParameters["rps"]?.toInt() ?: 10
                    val limit = call.request.queryParameters["limit"]?.toInt() ?: 20
                    val windowMs = call.request.queryParameters["windowMs"]?.toLong() ?: 5000L
                    call.respond(manager.runComparison(duration, rps, limit, windowMs))
                }
            }
        }
    }.start(wait = true)
}