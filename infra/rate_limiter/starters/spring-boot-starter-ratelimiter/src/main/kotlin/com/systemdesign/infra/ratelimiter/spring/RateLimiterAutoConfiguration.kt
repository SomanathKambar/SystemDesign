package com.systemdesign.infra.ratelimiter.spring

import com.systemdesign.infra.ratelimiter.core.RateLimiter
import com.systemdesign.infra.ratelimiter.core.MechanismAdapter
import com.systemdesign.infra.ratelimiter.engine.DecisionClient
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
    fun decisionClient(properties: RateLimiterProperties): DecisionClient {
        val policyJson = properties.policyJson ?: generateDefaultPolicyJson(properties)
        
        val builder = DecisionClient.builder()
            .withPolicyJson(policyJson)
            .withShadowMode(properties.shadowMode)
        
        if (properties.mode == RateLimiterProperties.ExecutionMode.SIMULATION) {
            builder.withSimulationMode()
        } else {
            builder.withOperationalMode()
        }
        
        return builder.build()
    }

    @Bean
    @ConditionalOnMissingBean
    fun rateLimiter(decisionClient: DecisionClient): RateLimiter {
        // Bridge for legacy code that expects RateLimiter interface
        return object : RateLimiter {
            override fun allow(key: String): com.systemdesign.infra.ratelimiter.core.model.Decision {
                return decisionClient.evaluate(key)
            }
        }
    }

    private fun generateDefaultPolicyJson(properties: RateLimiterProperties): String {
        val use = when (properties.type) {
            RateLimiterProperties.RateLimiterType.FIXED_WINDOW -> "fixed_window"
            RateLimiterProperties.RateLimiterType.SLIDING_WINDOW_COUNTER -> "sliding_window_counter"
            RateLimiterProperties.RateLimiterType.SLIDING_WINDOW_LOG -> "sliding_window_log"
            RateLimiterProperties.RateLimiterType.TOKEN_BUCKET -> "token_bucket"
        }
        
        val params = when (properties.type) {
            RateLimiterProperties.RateLimiterType.FIXED_WINDOW -> 
                "\"limit\": \"${properties.fixedWindow.limit}\", \"windowSizeMs\": \"${properties.fixedWindow.windowSizeMs}\""
            RateLimiterProperties.RateLimiterType.SLIDING_WINDOW_COUNTER, RateLimiterProperties.RateLimiterType.SLIDING_WINDOW_LOG ->
                "\"limit\": \"${properties.slidingWindow.limit}\", \"windowSizeMs\": \"${properties.slidingWindow.windowSizeMs}\""
            RateLimiterProperties.RateLimiterType.TOKEN_BUCKET ->
                "\"capacity\": \"${properties.tokenBucket.capacity}\", \"refillRate\": \"${properties.tokenBucket.refillTokensPerSecond}\""
        }

        return """
            {
                "policies": [
                    {
                        "name": "default-policy",
                        "priority": 0,
                        "then": {
                            "use": "$use",
                            "params": { $params }
                        }
                    }
                ]
            }
        """.trimIndent()
    }
}
