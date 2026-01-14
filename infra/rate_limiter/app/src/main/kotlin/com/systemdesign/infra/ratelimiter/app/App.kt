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
            }
        }
    }.start(wait = true)
}