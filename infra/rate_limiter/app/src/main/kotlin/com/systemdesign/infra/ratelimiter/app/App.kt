package com.systemdesign.infra.ratelimiter.app

import com.systemdesign.infra.ratelimiter.strategy.fixedwindow.FixedWindowRateLimiter
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
data class ConfigRequest(val limit: Int, val windowSizeMs: Long)

@Serializable
data class RateLimitRequest(val key: String)

@Serializable
data class RateLimitResponse(
    val allowed: Boolean,
    val retryAfterMs: Long? = null,
    val currentLimit: Int,
    val currentWindowSizeMs: Long
)

@Serializable
data class ConfigUpdateResponse(
    val status: String,
    val config: ConfigRequest
)

class RateLimiterManager {
    // Default config
    private var currentLimit = 5
    private var currentWindowSize = 10000L // 10 seconds
    
    private val limiterRef = AtomicReference(
        FixedWindowRateLimiter(currentLimit, currentWindowSize)
    )

    fun updateConfig(limit: Int, windowSizeMs: Long) {
        this.currentLimit = limit
        this.currentWindowSize = windowSizeMs
        // Resetting limiter with new config. Note: This clears state (InMemory). 
        // For a real persistent store, we might keep the store instance.
        limiterRef.set(FixedWindowRateLimiter(limit, windowSizeMs))
    }

    fun allow(key: String): RateLimitResponse {
        val decision = limiterRef.get().allow(key)
        return RateLimitResponse(
            allowed = decision.allowed,
            retryAfterMs = decision.retryAfterMs,
            currentLimit = currentLimit,
            currentWindowSizeMs = currentWindowSize
        )
    }
    
    fun getConfig() = ConfigRequest(currentLimit, currentWindowSize)
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
                    manager.updateConfig(config.limit, config.windowSizeMs)
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
