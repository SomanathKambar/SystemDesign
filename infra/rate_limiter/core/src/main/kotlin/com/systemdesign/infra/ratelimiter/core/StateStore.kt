package com.systemdesign.infra.ratelimiter.core

/**
 * Generic interface for state storage.
 */
interface StateStore<T> {
    fun get(key: String): T?
    fun save(key: String, state: T, ttlMs: Long)
    fun delete(key: String)

    /**
     * Atomically computes the new state based on the current state.
     */
    fun compute(key: String, ttlMs: Long, remappingFunction: (T?) -> T?): T?
}