package com.systemdesign.infra.ratelimiter.engine

import com.systemdesign.infra.ratelimiter.core.Clock
import com.systemdesign.infra.ratelimiter.core.Mechanism
import com.systemdesign.infra.ratelimiter.core.ShadowMechanism
import com.systemdesign.infra.ratelimiter.core.SystemClock
import com.systemdesign.infra.ratelimiter.core.dsl.ControlLaw
import com.systemdesign.infra.ratelimiter.core.dsl.PolicyLoader
import com.systemdesign.infra.ratelimiter.core.dsl.PolicyRouter
import com.systemdesign.infra.ratelimiter.core.model.Decision
import com.systemdesign.infra.ratelimiter.core.model.RequestContext
import com.systemdesign.infra.ratelimiter.engine.governance.CapabilityValidator
import com.systemdesign.infra.ratelimiter.engine.governance.OperationalValidator
import com.systemdesign.infra.ratelimiter.engine.governance.SimulationValidator

/**
 * Public SDK Facade for the Decision Engine.
 */
class DecisionClient private constructor(
    private val engine: Mechanism
) {

    /**
     * Evaluates a request against the configured policies and returns a decision.
     */
    fun evaluate(key: String, metrics: Map<String, Double> = emptyMap()): Decision {
        val context = RequestContext(key = key, metrics = metrics)
        return engine.execute(context)
    }

    class Builder {
        private var controlLaw: ControlLaw? = null
        private var validator: CapabilityValidator = OperationalValidator()
        private var clock: Clock = SystemClock()
        private var mechanismFactory: MechanismFactory? = null
        private var shadowMode: Boolean = false

        fun withPolicyJson(json: String) = apply {
            this.controlLaw = PolicyLoader.loadFromJson(json)
        }

        fun withSimulationMode() = apply {
            this.validator = SimulationValidator()
        }

        fun withOperationalMode() = apply {
            this.validator = OperationalValidator()
        }

        fun withShadowMode(enabled: Boolean = true) = apply {
            this.shadowMode = enabled
        }

        fun withClock(clock: Clock) = apply {
            this.clock = clock
        }

        fun withMechanismFactory(factory: MechanismFactory) = apply {
            this.mechanismFactory = factory
        }

        fun build(): DecisionClient {
            val law = controlLaw ?: throw IllegalStateException("ControlLaw must be provided")
            val factory = mechanismFactory ?: DefaultMechanismFactory(clock)
            val router = PolicyRouter(law)
            
            var engine: Mechanism = ControlPlane(router, factory, validator)
            
            if (shadowMode) {
                engine = ShadowMechanism(engine)
            }
            
            return DecisionClient(engine)
        }
    }

    companion object {
        fun builder() = Builder()
    }
}