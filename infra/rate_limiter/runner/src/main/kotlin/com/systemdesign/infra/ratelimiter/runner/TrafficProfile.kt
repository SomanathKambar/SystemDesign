package com.systemdesign.infra.ratelimiter.runner

import kotlinx.serialization.Serializable

@Serializable
sealed class TrafficProfile {
    abstract val name: String
    
    @Serializable
    data class Constant(
        override val name: String,
        val requestsPerSecond: Double,
        val durationMs: Long
    ) : TrafficProfile()

    @Serializable
    data class Burst(
        override val name: String,
        val burstSize: Int,
        val intervalMs: Long,
        val durationMs: Long
    ) : TrafficProfile()

    @Serializable
    data class Random(
        override val name: String,
        val avgRequestsPerSecond: Double,
        val durationMs: Long
    ) : TrafficProfile()

    @Serializable
    data class Boundary(
        override val name: String,
        val windowSizeMs: Long,
        val burstSize: Int,
        val durationMs: Long
    ) : TrafficProfile()
}
