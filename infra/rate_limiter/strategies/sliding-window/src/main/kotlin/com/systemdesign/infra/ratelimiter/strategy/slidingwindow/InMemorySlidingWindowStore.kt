package com.systemdesign.infra.ratelimiter.strategy.slidingwindow

import com.systemdesign.infra.ratelimiter.core.SlidingWindowStore
import com.systemdesign.infra.ratelimiter.core.model.SlidingWindowLog
import java.util.concurrent.ConcurrentHashMap

class InMemorySlidingWindowStore : SlidingWindowStore {
    private val store = ConcurrentHashMap<String, List<Long>>()

    override fun getLog(key: String): SlidingWindowLog? {
        return store[key]?.let { SlidingWindowLog(it) }
    }

    override fun saveLog(key: String, log: SlidingWindowLog, ttlMs: Long) {
        store[key] = log.timestamps
    }

    override fun removeOldEntries(key: String, beforeTimestamp: Long) {
        store.computeIfPresent(key) { _, timestamps ->
            timestamps.filter { it >= beforeTimestamp }
        }
    }
}
