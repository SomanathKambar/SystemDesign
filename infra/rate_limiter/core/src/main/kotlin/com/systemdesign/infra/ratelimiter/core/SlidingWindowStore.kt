package com.systemdesign.infra.ratelimiter.core

import com.systemdesign.infra.ratelimiter.core.model.SlidingWindowLog

interface SlidingWindowStore {
    fun getLog(key: String): SlidingWindowLog?
    fun saveLog(key: String, log: SlidingWindowLog, ttlMs: Long)
    fun removeOldEntries(key: String, beforeTimestamp: Long)
}
