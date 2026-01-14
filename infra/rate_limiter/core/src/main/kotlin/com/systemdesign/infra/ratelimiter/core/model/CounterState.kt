package com.systemdesign.infra.ratelimiter.core.model

data class CounterState(
    val count: Long,
    val windowStart: Long
)
