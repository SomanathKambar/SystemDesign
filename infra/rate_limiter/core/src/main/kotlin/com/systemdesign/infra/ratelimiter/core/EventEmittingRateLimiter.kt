package com.systemdesign.infra.ratelimiter.core

import com.systemdesign.infra.ratelimiter.core.event.RateLimitEvent
import com.systemdesign.infra.ratelimiter.core.model.Decision
import java.util.UUID

/**
 * A wrapper for [RateLimiter] that emits events for every decision, including internal state changes.
 */
class EventEmittingRateLimiter(
    private val delegate: RateLimiter,
    private val strategyName: String,
    private val nodeId: String,
    private val clock: Clock,
    private val eventConsumer: (RateLimitEvent) -> Unit
) : RateLimiter {

    override fun allow(key: String): Decision {
        val decision = delegate.allow(key)
        val timestamp = clock.currentTimeMillis()
        
        // 1. Check for internal state events in context
        emitInternalEvents(decision, timestamp)

        // 2. Emit the primary request event
        val eventId = UUID.randomUUID().toString()
        val event = if (decision.allowed) {
            RateLimitEvent.RequestAllowed(
                eventId = eventId,
                timestampMs = timestamp,
                strategy = strategyName,
                nodeId = nodeId,
                payload = decision.context.mapValues { it.value.toString() } + ("key" to key)
            )
        } else {
            RateLimitEvent.RequestBlocked(
                eventId = eventId,
                timestampMs = timestamp,
                strategy = strategyName,
                nodeId = nodeId,
                reason = "Rate limit exceeded",
                payload = decision.context.mapValues { it.value.toString() } + mapOf(
                    "key" to key,
                    "retryAfterMs" to (decision.retryAfterMs?.toString() ?: "0")
                )
            )
        }

        eventConsumer(event)
        return decision
    }

    private fun emitInternalEvents(decision: Decision, timestamp: Long) {
        val ctx = decision.context

        // Token Bucket Refill
        if (ctx.containsKey("tokensAdded") && (ctx["tokensAdded"] as? Double ?: 0.0) > 0.0) {
            eventConsumer(
                RateLimitEvent.TokenRefilled(
                    eventId = UUID.randomUUID().toString(),
                    timestampMs = timestamp,
                    strategy = strategyName,
                    nodeId = nodeId,
                    tokensAdded = ctx["tokensAdded"] as Double,
                    currentTokens = ctx["tokensAfterRefill"] as Double
                )
            )
        }

        // Window Shift (Fixed/Sliding)
        if (ctx["isNewWindow"] == true) {
            eventConsumer(
                RateLimitEvent.WindowShifted(
                    eventId = UUID.randomUUID().toString(),
                    timestampMs = timestamp,
                    strategy = strategyName,
                    nodeId = nodeId,
                    newWindowStartMs = ctx["windowStart"] as Long
                )
            )
        }

        // Leaky Bucket Leak
        if (ctx.containsKey("leakedAmount") && (ctx["leakedAmount"] as? Double ?: 0.0) >= 0.0) {
            eventConsumer(
                RateLimitEvent.LeakOccurred(
                    eventId = UUID.randomUUID().toString(),
                    timestampMs = timestamp,
                    strategy = strategyName,
                    nodeId = nodeId,
                    leakedAmount = ctx["leakedAmount"] as Double,
                    waterLevelAfterLeak = ctx["waterAfterLeak"] as Double
                )
            )
        }
    }
    
    fun emitTick() {
        eventConsumer(
            RateLimitEvent.Tick(
                eventId = UUID.randomUUID().toString(),
                timestampMs = clock.currentTimeMillis(),
                strategy = strategyName,
                nodeId = nodeId
            )
        )
    }
}