package com.systemdesign.infra.ratelimiter.strategy.fixedwindow

import com.systemdesign.infra.ratelimiter.core.StateStore
import com.systemdesign.infra.ratelimiter.core.model.FixedWindowCounter
import java.util.concurrent.ConcurrentHashMap

class InMemoryStateStore : StateStore<FixedWindowCounter> {
    private val storage = ConcurrentHashMap<String, FixedWindowCounter>()

    override fun get(key: String): FixedWindowCounter? = storage[key]

    override fun save(key: String, state: FixedWindowCounter, ttlMs: Long) {
        storage[key] = state
    }

    override fun delete(key: String) {
        storage.remove(key)
    }

    override fun compute(
        key: String,
        ttlMs: Long,
        remappingFunction: (FixedWindowCounter?) -> FixedWindowCounter?
    ): FixedWindowCounter? {
        return storage.compute(key) { _, current -> remappingFunction(current) }
    }
}