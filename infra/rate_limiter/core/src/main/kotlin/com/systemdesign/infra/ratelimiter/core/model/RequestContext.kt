package com.systemdesign.infra.ratelimiter.core.model

data class RequestContext(
    val key: String,
    val tokens: Long = 1, // Cost of the request
    val metadata: Map<String, Any> = emptyMap(), // Headers, etc.
    val metrics: Map<String, Double> = emptyMap() // Current system state (CPU, RPS)
)
