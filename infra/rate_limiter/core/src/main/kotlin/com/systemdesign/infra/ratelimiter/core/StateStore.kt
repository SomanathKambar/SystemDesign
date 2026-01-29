package com.systemdesign.infra.ratelimiter.core

import com.systemdesign.infra.ratelimiter.core.model.CounterState

interface StateStore {
    fun get(key: String): CounterState?
    fun save(key: String, state: CounterState, ttlMs: Long)
    fun delete(key: String)

    /**
     * Atomically computes the new state based on the current state.
     */
    fun compute(key: String, ttlMs: Long, remappingFunction: (CounterState?) -> CounterState?): CounterState?
}
