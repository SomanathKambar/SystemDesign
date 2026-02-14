package com.systemdesign.infra.ratelimiter.core.model

data class Decision(
    val allowed: Boolean,
    val reason: String, // "Window full", "Token bucket empty", "Allowed"
    val retryAfterMs: Long? = null,
    val metadata: Map<String, Any> = emptyMap() // Detailed trace/context
)