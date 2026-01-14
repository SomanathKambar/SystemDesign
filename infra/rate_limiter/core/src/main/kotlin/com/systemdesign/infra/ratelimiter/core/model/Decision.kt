package com.systemdesign.infra.ratelimiter.core.model

data class Decision(
    val allowed: Boolean,
    val retryAfterMs: Long? = null,
    val context: Map<String, Any> = emptyMap()
)
