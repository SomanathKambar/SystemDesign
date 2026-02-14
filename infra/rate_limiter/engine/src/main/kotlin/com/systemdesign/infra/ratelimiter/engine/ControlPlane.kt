package com.systemdesign.infra.ratelimiter.engine

import com.systemdesign.infra.ratelimiter.core.Clock
import com.systemdesign.infra.ratelimiter.core.Mechanism
import com.systemdesign.infra.ratelimiter.core.SystemClock
import com.systemdesign.infra.ratelimiter.core.dsl.Action
import com.systemdesign.infra.ratelimiter.core.dsl.PolicyRouter
import com.systemdesign.infra.ratelimiter.core.model.Decision
import com.systemdesign.infra.ratelimiter.core.model.RequestContext
import com.systemdesign.infra.ratelimiter.engine.governance.CapabilityValidator
import com.systemdesign.infra.ratelimiter.engine.governance.InstabilityRiskException
import com.systemdesign.infra.ratelimiter.engine.governance.SimulationValidator
import com.systemdesign.infra.ratelimiter.strategy.fixedwindow.FixedWindowMechanism
import com.systemdesign.infra.ratelimiter.strategy.tokenbucket.TokenBucketMechanism
import com.systemdesign.infra.ratelimiter.strategy.leakybucket.LeakyBucketMechanism
import com.systemdesign.infra.ratelimiter.strategy.slidingwindow.SlidingWindowMechanism
import com.systemdesign.infra.ratelimiter.strategy.slidingwindow.SlidingWindowLogMechanism

interface MechanismFactory {
    fun getMechanism(action: Action): Mechanism
}

/**
 * The ControlPlane is the central orchestrator that applies Control Laws (Policies)
 * to select and execute the appropriate Mechanism, while enforcing governance via validators.
 */
class ControlPlane(
    private val router: PolicyRouter,
    private val factory: MechanismFactory,
    private val validator: CapabilityValidator = SimulationValidator()
) : Mechanism {

    override fun execute(context: RequestContext): Decision {
        val policy = router.match(context) 
            ?: throw IllegalStateException("No policy matched for context: $context")
            
        val validation = validator.validate(policy.action)
        if (!validation.isValid) {
            // Rejection in Operational mode
            throw InstabilityRiskException(validation.message)
        }
        
        // Note: Warnings in Simulation mode can be logged or emitted as events.
        // For now, we proceed to execution.

        val mechanism = factory.getMechanism(policy.action)
        return mechanism.execute(context)
    }
}

class DefaultMechanismFactory(
    private val clock: Clock = SystemClock()
) : MechanismFactory {
    
    private val cache = mutableMapOf<String, Mechanism>()

    override fun getMechanism(action: Action): Mechanism {
        val cacheKey = "${action.use}-${action.params}"
        return cache.getOrPut(cacheKey) {
            createMechanism(action)
        }
    }

    private fun createMechanism(action: Action): Mechanism {
        return when (action.use.lowercase()) {
            "fixed_window" -> {
                val windowSize = action.params["windowSizeMs"]?.toLong() ?: 1000L
                val limit = action.params["limit"]?.toInt() ?: 10
                FixedWindowMechanism(windowSize, limit, clock = clock)
            }
            "token_bucket" -> {
                val capacity = action.params["capacity"]?.toDouble() ?: 10.0
                val refill = action.params["refillRate"]?.toDouble() ?: 1.0
                TokenBucketMechanism(capacity, refill, clock = clock)
            }
            "leaky_bucket" -> {
                val capacity = action.params["capacity"]?.toDouble() ?: 10.0
                val leakRate = action.params["leakRate"]?.toDouble() ?: 1.0
                LeakyBucketMechanism(capacity, leakRate, clock = clock)
            }
            "sliding_window", "sliding_window_counter" -> {
                val windowSize = action.params["windowSizeMs"]?.toLong() ?: 1000L
                val limit = action.params["limit"]?.toInt() ?: 10
                SlidingWindowMechanism(windowSize, limit, clock = clock)
            }
            "sliding_window_log" -> {
                val windowSize = action.params["windowSizeMs"]?.toLong() ?: 1000L
                val limit = action.params["limit"]?.toInt() ?: 10
                SlidingWindowLogMechanism(windowSize, limit, clock = clock)
            }
            else -> throw IllegalArgumentException("Unknown mechanism: ${action.use}")
        }
    }
}