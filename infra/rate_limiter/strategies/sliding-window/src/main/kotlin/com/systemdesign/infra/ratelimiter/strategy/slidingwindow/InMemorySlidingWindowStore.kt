package com.systemdesign.infra.ratelimiter.strategy.slidingwindow

import com.systemdesign.infra.ratelimiter.core.SlidingWindowStore
import com.systemdesign.infra.ratelimiter.core.model.SlidingWindowLog
import java.util.concurrent.ConcurrentHashMap

class InMemorySlidingWindowStore : SlidingWindowStore {
    private val store = ConcurrentHashMap<String, List<Long>>()

    override fun get(key: String): SlidingWindowLog? = getLog(key)

    override fun save(key: String, state: SlidingWindowLog, ttlMs: Long) = saveLog(key, state, ttlMs)

    override fun delete(key: String) {
        store.remove(key)
    }

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

    override fun compute(
        key: String,
        ttlMs: Long,
        remappingFunction: (SlidingWindowLog?) -> SlidingWindowLog?
    ): SlidingWindowLog? {
        val result = store.compute(key) { _, timestamps ->
            val log = timestamps?.let { SlidingWindowLog(it) }
            val newLog = remappingFunction(log)
            newLog?.timestamps
        }
        return result?.let { SlidingWindowLog(it) }
    }
}