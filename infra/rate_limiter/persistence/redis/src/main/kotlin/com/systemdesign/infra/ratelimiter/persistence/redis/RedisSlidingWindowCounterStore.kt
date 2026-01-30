package com.systemdesign.infra.ratelimiter.persistence.redis

import com.systemdesign.infra.ratelimiter.core.StateStore
import com.systemdesign.infra.ratelimiter.core.model.SlidingWindowCounter
import redis.clients.jedis.Jedis
import redis.clients.jedis.Transaction

class RedisSlidingWindowCounterStore(private val jedis: Jedis) : StateStore<SlidingWindowCounter> {

    override fun get(key: String): SlidingWindowCounter? {
        val data = jedis.hgetAll(key)
        if (data.isEmpty()) return null
        
        return SlidingWindowCounter(
            count = data["count"]?.toInt() ?: 0,
            windowStart = data["windowStart"]?.toLong() ?: 0L,
            previousCount = data["previousCount"]?.toInt() ?: 0
        )
    }

    override fun save(key: String, state: SlidingWindowCounter, ttlMs: Long) {
        val map = mapOf(
            "count" to state.count.toString(),
            "windowStart" to state.windowStart.toString(),
            "previousCount" to state.previousCount.toString()
        )
        jedis.hmset(key, map)
        jedis.pexpire(key, ttlMs)
    }

    override fun delete(key: String) {
        jedis.del(key)
    }

    override fun compute(
        key: String,
        ttlMs: Long,
        remappingFunction: (SlidingWindowCounter?) -> SlidingWindowCounter?
    ): SlidingWindowCounter? {
        while (true) {
            jedis.watch(key)
            val currentState = get(key)
            val newState = remappingFunction(currentState)
            
            val t: Transaction = jedis.multi()
            if (newState == null) {
                t.del(key)
            } else {
                val map = mapOf(
                    "count" to newState.count.toString(),
                    "windowStart" to newState.windowStart.toString(),
                    "previousCount" to newState.previousCount.toString()
                )
                t.hmset(key, map)
                t.pexpire(key, ttlMs)
            }
            
            val results = t.exec()
            if (results != null) {
                return newState
            }
        }
    }
}
