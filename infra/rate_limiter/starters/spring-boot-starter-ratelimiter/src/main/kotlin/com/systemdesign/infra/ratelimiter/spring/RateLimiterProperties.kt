package com.systemdesign.infra.ratelimiter.spring

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "rate-limiter")
class RateLimiterProperties {
    var enabled: Boolean = true
    var type: RateLimiterType = RateLimiterType.FIXED_WINDOW
    var storeType: StoreType = StoreType.IN_MEMORY
    var mode: ExecutionMode = ExecutionMode.OPERATIONAL
    var shadowMode: Boolean = false
    
    // New: Policy-driven config
    var policyJson: String? = null

    var redis = RedisProperties()
    var fixedWindow = FixedWindowProperties()
    var slidingWindow = SlidingWindowProperties()
    var tokenBucket = TokenBucketProperties()

    enum class RateLimiterType {
        FIXED_WINDOW,
        SLIDING_WINDOW_COUNTER,
        SLIDING_WINDOW_LOG,
        TOKEN_BUCKET
    }

    enum class StoreType {
        IN_MEMORY,
        REDIS
    }

    enum class ExecutionMode {
        SIMULATION,
        OPERATIONAL
    }

    class RedisProperties {
        var host: String = "localhost"
        var port: Int = 6379
    }

    class FixedWindowProperties {
        var limit: Int = 100
        var windowSizeMs: Long = 60000
    }

    class SlidingWindowProperties {
        var limit: Int = 100
        var windowSizeMs: Long = 60000
    }

    class TokenBucketProperties {
        var capacity: Double = 100.0
        var refillTokensPerSecond: Double = 10.0
    }
}