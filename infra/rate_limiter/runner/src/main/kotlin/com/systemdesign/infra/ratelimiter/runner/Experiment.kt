package com.systemdesign.infra.ratelimiter.runner

import kotlinx.serialization.Serializable

@Serializable
data class ExperimentMetadata(
    val id: String,
    val name: String,
    val description: String,
    val strategy: String,
    val config: Map<String, String>,
    val profile: TrafficProfile,
    val timestamp: Long
)

@Serializable
data class ExperimentBundle(
    val metadata: ExperimentMetadata,
    val eventsPath: String
)
