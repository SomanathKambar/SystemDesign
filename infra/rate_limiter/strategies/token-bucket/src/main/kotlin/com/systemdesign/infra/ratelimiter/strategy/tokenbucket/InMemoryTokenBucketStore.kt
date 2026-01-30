package com.systemdesign.infra.ratelimiter.strategy.tokenbucket

import com.systemdesign.infra.ratelimiter.core.TokenBucketStore
import com.systemdesign.infra.ratelimiter.core.model.TokenBucketState
import java.util.concurrent.ConcurrentHashMap

class InMemoryTokenBucketStore : TokenBucketStore {
    private val store = ConcurrentHashMap<String, TokenBucketState>()

    override fun get(key: String): TokenBucketState? = store[key]

    override fun save(key: String, state: TokenBucketState, ttlMs: Long) {
        store[key] = state
    }

    override fun delete(key: String) {
        store.remove(key)
    }

    override fun compute(
        key: String,
        ttlMs: Long,
        remappingFunction: (TokenBucketState?) -> TokenBucketState?
    ): TokenBucketState? {
        return store.compute(key) { _, currentState ->
            remappingFunction(currentState)
        }
    }
}