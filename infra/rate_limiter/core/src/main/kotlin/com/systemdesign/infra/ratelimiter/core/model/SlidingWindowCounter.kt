package com.systemdesign.infra.ratelimiter.core.model

data class SlidingWindowCounter(
    val count: Int,
    val windowStart: Long,
    val previousCount: Int
)
