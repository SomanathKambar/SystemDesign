package com.systemdesign.infra.ratelimiter.core

import com.systemdesign.infra.ratelimiter.core.model.TokenBucketState

interface TokenBucketStore {
    fun get(key: String): TokenBucketState?
    fun save(key: String, state: TokenBucketState)
}
