package com.systemdesign.infra.ratelimiter.persistence.redis

import com.systemdesign.infra.ratelimiter.core.SlidingWindowStore
import com.systemdesign.infra.ratelimiter.core.model.SlidingWindowLog
import redis.clients.jedis.Jedis
import java.util.UUID

class RedisSlidingWindowStore(private val jedis: Jedis) : SlidingWindowStore {

    override fun getLog(key: String): SlidingWindowLog? {
        val members = jedis.zrange(key, 0, -1)
        if (members.isEmpty()) return null
        
        // Members are "timestamp:uuid"
        val timestamps = members.map { it.split(":")[0].toLong() }
        return SlidingWindowLog(timestamps)
    }

    override fun saveLog(key: String, log: SlidingWindowLog, ttlMs: Long) {
        // This method in the interface seems to imply saving the WHOLE log.
        // For Redis ZSET, we usually add entries one by one.
        // But to satisfy the interface:
        jedis.del(key)
        val pipeline = jedis.pipelined()
        log.timestamps.forEach { ts ->
            pipeline.zadd(key, ts.toDouble(), "$ts:${UUID.randomUUID()}")
        }
        pipeline.pexpire(key, ttlMs)
        pipeline.sync()
    }

    override fun removeOldEntries(key: String, beforeTimestamp: Long) {
        jedis.zremrangeByScore(key, 0.0, (beforeTimestamp - 1).toDouble())
    }

    override fun compute(
        key: String,
        ttlMs: Long,
        remappingFunction: (SlidingWindowLog?) -> SlidingWindowLog?
    ): SlidingWindowLog? {
        while (true) {
            jedis.watch(key)
            val currentLog = getLog(key)
            val newLog = remappingFunction(currentLog)

            val t = jedis.multi()
            if (newLog == null) {
                t.del(key)
            } else {
                // This is still inefficient but follows the interface.
                // In a real optimized version, we'd only add the delta.
                t.del(key)
                newLog.timestamps.forEach { ts ->
                    t.zadd(key, ts.toDouble(), "$ts:${UUID.randomUUID()}")
                }
                t.pexpire(key, ttlMs)
            }

            val results = t.exec()
            if (results != null) {
                return newLog
            }
        }
    }
    
    // We should probably add an 'addEntry' method to SlidingWindowStore to avoid fetching/saving whole log
    fun addEntry(key: String, timestamp: Long, ttlMs: Long) {
        jedis.zadd(key, timestamp.toDouble(), "$timestamp:${UUID.randomUUID()}")
        jedis.pexpire(key, ttlMs)
    }
}
