package com.systemdesign.infra.ratelimiter.strategy.fixedwindow

import com.systemdesign.infra.ratelimiter.core.RateLimiter
import com.systemdesign.infra.ratelimiter.core.StateStore
import com.systemdesign.infra.ratelimiter.core.model.CounterState
import com.systemdesign.infra.ratelimiter.core.model.Decision
import java.util.concurrent.ConcurrentHashMap

class InMemoryStateStore : StateStore {
    private val store = ConcurrentHashMap<String, CounterState>()

    override fun get(key: String): CounterState? {
        return store[key]
    }

    override fun save(key: String, state: CounterState, ttlMs: Long) {
        // In a real Redis store, we would set TTL. 
        // For in-memory, we rely on the logic to overwrite/ignore old windows or run a cleanup job.
        store[key] = state
    }

    override fun delete(key: String) {
        store.remove(key)
    }
}
