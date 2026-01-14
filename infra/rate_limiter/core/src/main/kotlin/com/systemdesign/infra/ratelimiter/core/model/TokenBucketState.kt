package com.systemdesign.infra.ratelimiter.core.model

data class TokenBucketState(
    val tokens: Double,
    val lastRefillTime: Long
)
