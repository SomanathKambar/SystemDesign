package com.systemdesign.infra.ratelimiter.core.model

data class LeakyBucketState(
    val waterLevel: Double,
    val lastLeakTime: Long
)
