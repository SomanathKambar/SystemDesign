package com.systemdesign.infra.ratelimiter.core

import com.systemdesign.infra.ratelimiter.core.model.CounterState

interface StateStore {
    fun get(key: String): CounterState?
    fun save(key: String, state: CounterState, ttlMs: Long)
    fun delete(key: String)
}
