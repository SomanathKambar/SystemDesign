package com.systemdesign.infra.ratelimiter.strategy.slidingwindow

import com.systemdesign.infra.ratelimiter.core.StateStore
import com.systemdesign.infra.ratelimiter.core.model.SlidingWindowCounter
import java.util.concurrent.ConcurrentHashMap

class InMemoryStateStore : StateStore<SlidingWindowCounter> {
    private val storage = ConcurrentHashMap<String, SlidingWindowCounter>()

    override fun get(key: String): SlidingWindowCounter? = storage[key]

    override fun save(key: String, state: SlidingWindowCounter, ttlMs: Long) {
        storage[key] = state
    }

    override fun delete(key: String) {
        storage.remove(key)
    }

    override fun compute(
        key: String,
        ttlMs: Long,
        remappingFunction: (SlidingWindowCounter?) -> SlidingWindowCounter?
    ): SlidingWindowCounter? {
        return storage.compute(key) { _, current -> remappingFunction(current) }
    }
}
