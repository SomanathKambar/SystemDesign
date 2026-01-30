package com.systemdesign.infra.ratelimiter.core.event

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
sealed class RateLimitEvent {
    abstract val eventId: String
    abstract val timestampMs: Long
    abstract val strategy: String
    abstract val nodeId: String
    abstract val payload: Map<String, String>

    @Serializable
    @SerialName("REQUEST_ALLOWED")
    data class RequestAllowed(
        override val eventId: String,
        override val timestampMs: Long,
        override val strategy: String,
        override val nodeId: String,
        override val payload: Map<String, String> = emptyMap()
    ) : RateLimitEvent()

    @Serializable
    @SerialName("REQUEST_BLOCKED")
    data class RequestBlocked(
        override val eventId: String,
        override val timestampMs: Long,
        override val strategy: String,
        override val nodeId: String,
        val reason: String,
        override val payload: Map<String, String> = emptyMap()
    ) : RateLimitEvent()

    @Serializable
    @SerialName("TOKEN_REFILLED")
    data class TokenRefilled(
        override val eventId: String,
        override val timestampMs: Long,
        override val strategy: String,
        override val nodeId: String,
        val tokensAdded: Double,
        val currentTokens: Double,
        override val payload: Map<String, String> = emptyMap()
    ) : RateLimitEvent()

    @Serializable
    @SerialName("WINDOW_SHIFTED")
    data class WindowShifted(
        override val eventId: String,
        override val timestampMs: Long,
        override val strategy: String,
        override val nodeId: String,
        val newWindowStartMs: Long,
        override val payload: Map<String, String> = emptyMap()
    ) : RateLimitEvent()

        @Serializable

        @SerialName("TICK")

        data class Tick(

            override val eventId: String,

            override val timestampMs: Long,

            override val strategy: String,

            override val nodeId: String,

            override val payload: Map<String, String> = emptyMap()

        ) : RateLimitEvent()

    

        @Serializable

        @SerialName("LEAK_OCCURRED")

        data class LeakOccurred(

            override val eventId: String,

            override val timestampMs: Long,

            override val strategy: String,

            override val nodeId: String,

            val leakedAmount: Double,

            val waterLevelAfterLeak: Double,

            override val payload: Map<String, String> = emptyMap()

        ) : RateLimitEvent()

    }

    