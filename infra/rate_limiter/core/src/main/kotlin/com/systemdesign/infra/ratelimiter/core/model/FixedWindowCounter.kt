package com.systemdesign.infra.ratelimiter.core.model

data class FixedWindowCounter(
    val count: Int,
    val windowStart: Long
)
