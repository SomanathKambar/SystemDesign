package com.systemdesign.infra.ratelimiter.strategy.tokenbucket

import com.systemdesign.infra.ratelimiter.core.TokenBucketStore
import com.systemdesign.infra.ratelimiter.core.model.TokenBucketState
import java.util.concurrent.ConcurrentHashMap

class InMemoryTokenBucketStore : TokenBucketStore {
    private val store = ConcurrentHashMap<String, TokenBucketState>()

    override fun get(key: String): TokenBucketState? = store[key]

    override fun save(key: String, state: TokenBucketState) {
        store[key] = state
    }
}
