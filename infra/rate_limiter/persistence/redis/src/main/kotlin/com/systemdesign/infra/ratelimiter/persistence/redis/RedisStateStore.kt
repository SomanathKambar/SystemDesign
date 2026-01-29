package com.systemdesign.infra.ratelimiter.persistence.redis

import com.systemdesign.infra.ratelimiter.core.StateStore
import com.systemdesign.infra.ratelimiter.core.model.CounterState
import redis.clients.jedis.Jedis
import redis.clients.jedis.Transaction

class RedisStateStore(private val jedis: Jedis) : StateStore {

    override fun get(key: String): CounterState? {
        val data = jedis.hgetAll(key)
        if (data.isEmpty()) return null
        
        return CounterState(
            count = data["count"]?.toLong() ?: 0L,
            windowStart = data["windowStart"]?.toLong() ?: 0L
        )
    }

    override fun save(key: String, state: CounterState, ttlMs: Long) {
        val map = mapOf(
            "count" to state.count.toString(),
            "windowStart" to state.windowStart.toString()
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
        remappingFunction: (CounterState?) -> CounterState?
    ): CounterState? {
        // Implementation using WATCH/MULTI/EXEC for optimistic concurrency
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
                    "windowStart" to newState.windowStart.toString()
                )
                t.hmset(key, map)
                t.pexpire(key, ttlMs)
            }
            
            val results = t.exec()
            if (results != null) {
                return newState
            }
            // If results is null, it means the key was modified by another client, retry.
        }
    }
}
