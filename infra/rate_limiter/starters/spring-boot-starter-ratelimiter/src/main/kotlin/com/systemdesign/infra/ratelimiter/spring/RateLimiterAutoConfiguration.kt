package com.systemdesign.infra.ratelimiter.spring

import com.systemdesign.infra.ratelimiter.core.RateLimiter
import com.systemdesign.infra.ratelimiter.core.StateStore
import com.systemdesign.infra.ratelimiter.core.TokenBucketStore
import com.systemdesign.infra.ratelimiter.core.SlidingWindowStore
import com.systemdesign.infra.ratelimiter.persistence.redis.RedisStateStore
import com.systemdesign.infra.ratelimiter.persistence.redis.RedisTokenBucketStore
import com.systemdesign.infra.ratelimiter.persistence.redis.RedisSlidingWindowStore
import com.systemdesign.infra.ratelimiter.strategy.fixedwindow.FixedWindowRateLimiter
import com.systemdesign.infra.ratelimiter.strategy.fixedwindow.InMemoryStateStore
import com.systemdesign.infra.ratelimiter.strategy.slidingwindow.SlidingWindowRateLimiter
import com.systemdesign.infra.ratelimiter.strategy.slidingwindow.SlidingWindowLogRateLimiter
import com.systemdesign.infra.ratelimiter.strategy.slidingwindow.InMemorySlidingWindowStore
import com.systemdesign.infra.ratelimiter.strategy.tokenbucket.TokenBucketRateLimiter
import com.systemdesign.infra.ratelimiter.strategy.tokenbucket.InMemoryTokenBucketStore
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import redis.clients.jedis.Jedis

@AutoConfiguration
@EnableConfigurationProperties(RateLimiterProperties::class)
@ConditionalOnProperty(prefix = "rate-limiter", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class RateLimiterAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "rate-limiter", name = ["store-type"], havingValue = "REDIS")
    fun jedis(properties: RateLimiterProperties): Jedis {
        return Jedis(properties.redis.host, properties.redis.port)
    }

    @Bean
    @ConditionalOnMissingBean
    fun rateLimiter(properties: RateLimiterProperties, 
                    jedis: Jedis? = null): RateLimiter {
        
        return when (properties.type) {
            RateLimiterProperties.RateLimiterType.FIXED_WINDOW -> {
                val store = if (jedis != null) RedisStateStore(jedis) else InMemoryStateStore()
                FixedWindowRateLimiter(
                    limit = properties.fixedWindow.limit,
                    windowSizeMs = properties.fixedWindow.windowSizeMs,
                    stateStore = store
                )
            }
            RateLimiterProperties.RateLimiterType.SLIDING_WINDOW_COUNTER -> {
                val store = if (jedis != null) RedisStateStore(jedis) else InMemoryStateStore()
                SlidingWindowRateLimiter(
                    limit = properties.slidingWindow.limit,
                    windowSizeMs = properties.slidingWindow.windowSizeMs,
                    stateStore = store
                )
            }
            RateLimiterProperties.RateLimiterType.SLIDING_WINDOW_LOG -> {
                val store = if (jedis != null) RedisSlidingWindowStore(jedis) else InMemorySlidingWindowStore()
                SlidingWindowLogRateLimiter(
                    limit = properties.slidingWindow.limit,
                    windowSizeMs = properties.slidingWindow.windowSizeMs,
                    store = store
                )
            }
            RateLimiterProperties.RateLimiterType.TOKEN_BUCKET -> {
                val store = if (jedis != null) RedisTokenBucketStore(jedis) else InMemoryTokenBucketStore()
                TokenBucketRateLimiter(
                    capacity = properties.tokenBucket.capacity,
                    refillTokensPerSecond = properties.tokenBucket.refillTokensPerSecond,
                    store = store
                )
            }
        }
    }
}
