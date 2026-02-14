package com.systemdesign.infra.ratelimiter.core

import com.systemdesign.infra.ratelimiter.core.event.RateLimitEvent
import com.systemdesign.infra.ratelimiter.core.model.Decision
import com.systemdesign.infra.ratelimiter.core.model.RequestContext
import java.util.UUID

/**
 * A wrapper for [Mechanism] that emits events for every decision.
 */
class EventEmittingMechanism(
    private val delegate: Mechanism,
    private val strategyName: String,
    private val nodeId: String,
    private val clock: Clock,
    private val eventConsumer: (RateLimitEvent) -> Unit
) : Mechanism {

    override fun execute(context: RequestContext): Decision {
        val decision = delegate.execute(context)
        val timestamp = clock.currentTimeMillis()
        val key = context.key
        
        // 1. Check for internal state events in metadata
        emitInternalEvents(decision, timestamp)

        // 2. Emit the primary request event
        val eventId = UUID.randomUUID().toString()
        val event = if (decision.allowed) {
            RateLimitEvent.RequestAllowed(
                eventId = eventId,
                timestampMs = timestamp,
                strategy = strategyName,
                nodeId = nodeId,
                payload = decision.metadata.mapValues { it.value.toString() } + ("key" to key)
            )
        } else {
            RateLimitEvent.RequestBlocked(
                eventId = eventId,
                timestampMs = timestamp,
                strategy = strategyName,
                nodeId = nodeId,
                reason = decision.reason,
                payload = decision.metadata.mapValues { it.value.toString() } + mapOf(
                    "key" to key,
                    "retryAfterMs" to (decision.retryAfterMs?.toString() ?: "0")
                )
            )
        }

        eventConsumer(event)
        return decision
    }

    private fun emitInternalEvents(decision: Decision, timestamp: Long) {
        val meta = decision.metadata

        // Token Bucket Refill
        if (meta.containsKey("tokensAdded") && (meta["tokensAdded"] as? Double ?: 0.0) > 0.0) {
            eventConsumer(
                RateLimitEvent.TokenRefilled(
                    eventId = UUID.randomUUID().toString(),
                    timestampMs = timestamp,
                    strategy = strategyName,
                    nodeId = nodeId,
                    tokensAdded = meta["tokensAdded"] as Double,
                    currentTokens = meta["tokensAfterRefill"] as Double
                )
            )
        }

        // Window Shift (Fixed/Sliding)
        if (meta["isNewWindow"] == true) {
            eventConsumer(
                RateLimitEvent.WindowShifted(
                    eventId = UUID.randomUUID().toString(),
                    timestampMs = timestamp,
                    strategy = strategyName,
                    nodeId = nodeId,
                    newWindowStartMs = meta["windowStart"] as Long
                )
            )
        }

        // Leaky Bucket Leak
        if (meta.containsKey("leakedAmount") && (meta["leakedAmount"] as? Double ?: 0.0) >= 0.0) {
            eventConsumer(
                RateLimitEvent.LeakOccurred(
                    eventId = UUID.randomUUID().toString(),
                    timestampMs = timestamp,
                    strategy = strategyName,
                    nodeId = nodeId,
                    leakedAmount = meta["leakedAmount"] as Double,
                    waterLevelAfterLeak = meta["waterAfterLeak"] as Double
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
