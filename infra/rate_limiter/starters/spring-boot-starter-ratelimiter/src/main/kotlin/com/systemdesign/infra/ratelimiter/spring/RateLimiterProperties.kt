package com.systemdesign.infra.ratelimiter.spring

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "rate-limiter")
class RateLimiterProperties {
    var enabled: Boolean = true
    var type: RateLimiterType = RateLimiterType.FIXED_WINDOW
    var storeType: StoreType = StoreType.IN_MEMORY
    
    var fixedWindow: FixedWindowProperties = FixedWindowProperties()
    var slidingWindow: SlidingWindowProperties = SlidingWindowProperties()
    var tokenBucket: TokenBucketProperties = TokenBucketProperties()
    var redis: RedisProperties = RedisProperties()

    enum class RateLimiterType {
        FIXED_WINDOW, SLIDING_WINDOW_COUNTER, SLIDING_WINDOW_LOG, TOKEN_BUCKET
    }

    enum class StoreType {
        IN_MEMORY, REDIS
    }

    class FixedWindowProperties {
        var limit: Int = 10
        var windowSizeMs: Long = 1000
    }

    class SlidingWindowProperties {
        var limit: Int = 10
        var windowSizeMs: Long = 1000
    }

    class TokenBucketProperties {
        var capacity: Double = 10.0
        var refillTokensPerSecond: Double = 1.0
    }

    class RedisProperties {
        var host: String = "localhost"
        var port: Int = 6379
    }
}
