package com.systemdesign.infra.ratelimiter.strategy.leakybucket

import com.systemdesign.infra.ratelimiter.core.LeakyBucketStore
import com.systemdesign.infra.ratelimiter.core.model.LeakyBucketState
import java.util.concurrent.ConcurrentHashMap

class InMemoryLeakyBucketStore : LeakyBucketStore {
    private val storage = ConcurrentHashMap<String, LeakyBucketState>()

    override fun get(key: String): LeakyBucketState? = storage[key]

    override fun save(key: String, state: LeakyBucketState, ttlMs: Long) {
        storage[key] = state
    }

    override fun delete(key: String) {
        storage.remove(key)
    }

    override fun compute(
        key: String,
        ttlMs: Long,
        remappingFunction: (LeakyBucketState?) -> LeakyBucketState?
    ): LeakyBucketState? {
        return storage.compute(key) { _, current -> remappingFunction(current) }
    }
}
