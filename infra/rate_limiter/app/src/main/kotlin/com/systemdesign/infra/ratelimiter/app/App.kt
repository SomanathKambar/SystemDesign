package com.systemdesign.infra.ratelimiter.app

import com.systemdesign.infra.ratelimiter.core.Clock
import com.systemdesign.infra.ratelimiter.core.RateLimiter
import com.systemdesign.infra.ratelimiter.core.MechanismAdapter
import com.systemdesign.infra.ratelimiter.core.SystemClock
import com.systemdesign.infra.ratelimiter.engine.DecisionClient
import com.systemdesign.infra.ratelimiter.strategy.fixedwindow.FixedWindowMechanism
import com.systemdesign.infra.ratelimiter.strategy.fixedwindow.InMemoryStateStore as FixedInMemoryStateStore
import com.systemdesign.infra.ratelimiter.strategy.slidingwindow.SlidingWindowMechanism
import com.systemdesign.infra.ratelimiter.strategy.slidingwindow.SlidingWindowLogMechanism
import com.systemdesign.infra.ratelimiter.strategy.slidingwindow.InMemorySlidingWindowStore
import com.systemdesign.infra.ratelimiter.strategy.slidingwindow.InMemoryStateStore as SlidingInMemoryStateStore
import com.systemdesign.infra.ratelimiter.strategy.tokenbucket.TokenBucketMechanism
import com.systemdesign.infra.ratelimiter.strategy.tokenbucket.InMemoryTokenBucketStore
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*
import java.util.concurrent.atomic.AtomicReference

@Serializable
enum class StrategyType {
    FIXED_WINDOW,
    SLIDING_WINDOW_COUNTER,
    SLIDING_WINDOW_LOG,
    TOKEN_BUCKET,
    LEAKY_BUCKET
}

@Serializable
data class ConfigRequest(
    val limit: Int, 
    val windowSizeMs: Long,
    val strategy: StrategyType = StrategyType.FIXED_WINDOW,
    val capacity: Double = 10.0,
    val refillTokensPerSecond: Double = 1.0,
    val leakRate: Double = 1.0,
    val simulationMode: Boolean = true
)

@Serializable
data class RateLimitRequest(val key: String)

@Serializable
data class RateLimitResponse(
    val allowed: Boolean,
    val reason: String? = null,
    val retryAfterMs: Long? = null,
    val currentLimit: Int,
    val currentWindowSizeMs: Long,
    val serverTimeMs: Long,
    val strategy: StrategyType,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
data class TelemetryEvent(
    val type: String,
    val eventId: String,
    val timestampMs: Long,
    val strategy: StrategyType,
    val nodeId: String = "node-1",
    val payload: Map<String, String>,
    val allowed: Boolean,
    val reason: String? = null
)

@Serializable
data class ConfigUpdateResponse(
    val status: String,
    val config: ConfigRequest
)

@Serializable
data class ComparisonDataPoint(
    val second: Int, 
    val fixedAllowed: Int, 
    val slidingCounterAllowed: Int,
    val slidingLogAllowed: Int,
    val tokenBucketAllowed: Int
)

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

class TelemetryService {
    private val _events = MutableSharedFlow<TelemetryEvent>()
    val events = _events.asSharedFlow()

    suspend fun emit(event: TelemetryEvent) {
        _events.emit(event)
    }
}

class RateLimiterManager(private val telemetryService: TelemetryService) {
    private var currentConfig = ConfigRequest(5, 10000, StrategyType.FIXED_WINDOW)
    private val clock = SystemClock()
    
    private val clientRef = AtomicReference<DecisionClient>(
        createClient(currentConfig)
    )

    private fun createClient(config: ConfigRequest): DecisionClient {
        val strategyName = when (config.strategy) {
            StrategyType.FIXED_WINDOW -> "fixed_window"
            StrategyType.SLIDING_WINDOW_COUNTER -> "sliding_window"
            StrategyType.SLIDING_WINDOW_LOG -> "sliding_window_log"
            StrategyType.TOKEN_BUCKET -> "token_bucket"
            StrategyType.LEAKY_BUCKET -> "leaky_bucket"
        }
        
        val policyJson = """
        {
            "policies": [
                {
                    "name": "default",
                    "then": {
                        "use": "$strategyName",
                        "params": {
                            "limit": "${config.limit}",
                            "windowSizeMs": "${config.windowSizeMs}",
                            "capacity": "${config.capacity}",
                            "refillRate": "${config.refillTokensPerSecond}",
                            "leakRate": "${config.leakRate}"
                        }
                    }
                }
            ]
        }
        """.trimIndent()

        val builder = DecisionClient.builder()
            .withPolicyJson(policyJson)
            .withClock(clock)
        
        if (config.simulationMode) {
            builder.withSimulationMode()
        } else {
            builder.withOperationalMode()
        }
        
        return builder.build()
    }

    fun runComparison(durationSec: Int, rps: Int, limit: Int, windowMs: Long): ComparisonResponse {
        val timeSeries = mutableListOf<ComparisonDataPoint>()
        
        // Use custom clocks to simulate time passing without Thread.sleep
        class ManualClock(var millis: Long) : Clock {
            override fun currentTimeMillis(): Long = millis
        }

        val startTime = System.currentTimeMillis()
        val fixedClock = ManualClock(startTime)
        val slidingCounterClock = ManualClock(startTime)
        val slidingLogClock = ManualClock(startTime)
        val tokenBucketClock = ManualClock(startTime)

        val fixed = MechanismAdapter(FixedWindowMechanism(windowMs, limit, FixedInMemoryStateStore(), fixedClock))
        val slidingCounter = MechanismAdapter(SlidingWindowMechanism(windowMs, limit, SlidingInMemoryStateStore(), slidingCounterClock))
        val slidingLog = MechanismAdapter(SlidingWindowLogMechanism(windowMs, limit, InMemorySlidingWindowStore(), slidingLogClock))
        val tokenBucket = MechanismAdapter(TokenBucketMechanism(limit.toDouble(), (limit.toDouble() / (windowMs / 1000.0)), InMemoryTokenBucketStore(), tokenBucketClock))
        
        var totalFixedAllowed = 0
        var totalSlidingCounterAllowed = 0
        var totalSlidingLogAllowed = 0
        var totalTokenBucketAllowed = 0
        
        for (sec in 1..durationSec) {
            var secFixedAllowed = 0
            var secSlidingCounterAllowed = 0
            var secSlidingLogAllowed = 0
            var secTokenBucketAllowed = 0
            
            for (r in 1..rps) {
                // Spread requests within the second
                val offset = (r * (1000 / rps)).toLong()
                val currentMillis = startTime + (sec - 1).toLong() * 1000 + offset
                fixedClock.millis = currentMillis
                slidingCounterClock.millis = currentMillis
                slidingLogClock.millis = currentMillis
                tokenBucketClock.millis = currentMillis
                
                if (fixed.allow("comp").allowed) secFixedAllowed++
                if (slidingCounter.allow("comp").allowed) secSlidingCounterAllowed++
                if (slidingLog.allow("comp").allowed) secSlidingLogAllowed++
                if (tokenBucket.allow("comp").allowed) secTokenBucketAllowed++
            }
            
            totalFixedAllowed += secFixedAllowed
            totalSlidingCounterAllowed += secSlidingCounterAllowed
            totalSlidingLogAllowed += secSlidingLogAllowed
            totalTokenBucketAllowed += secTokenBucketAllowed
            timeSeries.add(ComparisonDataPoint(sec, secFixedAllowed, secSlidingCounterAllowed, secSlidingLogAllowed, secTokenBucketAllowed))
        }

        val results = listOf(
            ComparisonResult(
                StrategyType.FIXED_WINDOW, 
                totalFixedAllowed, 
                (durationSec * rps) - totalFixedAllowed,
                (totalFixedAllowed.toDouble() / durationSec),
                "Simple, but prone to bursts at window boundaries."
            ),
            ComparisonResult(
                StrategyType.SLIDING_WINDOW_COUNTER, 
                totalSlidingCounterAllowed, 
                (durationSec * rps) - totalSlidingCounterAllowed,
                (totalSlidingCounterAllowed.toDouble() / durationSec),
                "Approximates sliding window using weighted average of current and previous window."
            ),
            ComparisonResult(
                StrategyType.SLIDING_WINDOW_LOG,
                totalSlidingLogAllowed,
                (durationSec * rps) - totalSlidingLogAllowed,
                (totalSlidingLogAllowed.toDouble() / durationSec),
                "Exact sliding window, tracks every request timestamp. Higher memory usage."
            ),
            ComparisonResult(
                StrategyType.TOKEN_BUCKET,
                totalTokenBucketAllowed,
                (durationSec * rps) - totalTokenBucketAllowed,
                (totalTokenBucketAllowed.toDouble() / durationSec),
                "Allows bursts up to capacity and refills at a constant rate. Best for smoothing traffic."
            )
        )

        val winner = results.maxByOrNull { it.allowed }?.strategy ?: StrategyType.TOKEN_BUCKET
        
        return ComparisonResponse(
            results = results,
            winner = winner,
            logic = "Winner determined by throughput under the simulated load pattern.",
            timeSeries = timeSeries
        )
    }

    fun updateConfig(config: ConfigRequest) {
        this.currentConfig = config
        clientRef.set(createClient(config))
    }

    suspend fun allow(key: String): RateLimitResponse {
        val decision = clientRef.get().evaluate(key)
        val now = clock.currentTimeMillis()
        
        val telemetry = TelemetryEvent(
            type = if (decision.allowed) "REQUEST_ALLOWED" else "REQUEST_BLOCKED",
            eventId = UUID.randomUUID().toString(),
            timestampMs = now,
            strategy = currentConfig.strategy,
            payload = decision.metadata.mapValues { it.value.toString() },
            allowed = decision.allowed,
            reason = decision.reason
        )
        telemetryService.emit(telemetry)

        return RateLimitResponse(
            allowed = decision.allowed,
            reason = decision.reason,
            retryAfterMs = decision.retryAfterMs,
            currentLimit = currentConfig.limit,
            currentWindowSizeMs = currentConfig.windowSizeMs,
            serverTimeMs = now,
            strategy = currentConfig.strategy,
            metadata = decision.metadata.mapValues { it.value.toString() }
        )
    }
    
    fun getConfig() = currentConfig
}

fun main() {
    embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) {
            json()
        }
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
        }
        
        val telemetryService = TelemetryService()
        val manager = RateLimiterManager(telemetryService)

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

            webSocket("/ws/telemetry") {
                telemetryService.events.collect { event ->
                    send(Json.encodeToString(event))
                }
            }
        }
    }.start(wait = true)
}
