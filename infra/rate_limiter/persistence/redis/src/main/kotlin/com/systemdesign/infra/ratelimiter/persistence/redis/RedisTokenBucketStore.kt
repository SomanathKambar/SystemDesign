package com.systemdesign.infra.ratelimiter.persistence.redis

import com.systemdesign.infra.ratelimiter.core.TokenBucketStore
import com.systemdesign.infra.ratelimiter.core.model.TokenBucketState
import redis.clients.jedis.Jedis
import redis.clients.jedis.Transaction

class RedisTokenBucketStore(private val jedis: Jedis) : TokenBucketStore {

    override fun get(key: String): TokenBucketState? {
        val data = jedis.hgetAll(key)
        if (data.isEmpty()) return null

        return TokenBucketState(
            tokens = data["tokens"]?.toDouble() ?: 0.0,
            lastRefillTime = data["lastRefillTime"]?.toLong() ?: 0L
        )
    }

    override fun save(key: String, state: TokenBucketState, ttlMs: Long) {
        val map = mapOf(
            "tokens" to state.tokens.toString(),
            "lastRefillTime" to state.lastRefillTime.toString()
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
        remappingFunction: (TokenBucketState?) -> TokenBucketState?
    ): TokenBucketState? {
        while (true) {
            jedis.watch(key)
            val currentState = get(key)
            val newState = remappingFunction(currentState)

            val t: Transaction = jedis.multi()
            if (newState == null) {
                t.del(key)
            } else {
                val map = mapOf(
                    "tokens" to newState.tokens.toString(),
                    "lastRefillTime" to newState.lastRefillTime.toString()
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
