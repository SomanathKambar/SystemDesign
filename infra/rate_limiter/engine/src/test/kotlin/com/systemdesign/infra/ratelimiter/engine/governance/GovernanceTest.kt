package com.systemdesign.infra.ratelimiter.engine.governance

import com.systemdesign.infra.ratelimiter.core.dsl.Action
import com.systemdesign.infra.ratelimiter.core.dsl.ControlLaw
import com.systemdesign.infra.ratelimiter.core.dsl.Policy
import com.systemdesign.infra.ratelimiter.core.dsl.PolicyRouter
import com.systemdesign.infra.ratelimiter.core.model.RequestContext
import com.systemdesign.infra.ratelimiter.engine.ControlPlane
import com.systemdesign.infra.ratelimiter.engine.DefaultMechanismFactory
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GovernanceTest {

    @Test
    fun `Simulation mode should allow unsafe configs with warning`() {
        val action = Action("fixed_window", mapOf("windowSizeMs" to "100")) // Unsafe < 1s
        val validator = SimulationValidator()
        val result = validator.validate(action)
        
        assertTrue(result.isValid)
        assertTrue(result.isWarning)
        assertTrue(result.message.contains("Warning"))
    }

    @Test
    fun `Operational mode should reject unsafe configs`() {
        val action = Action("fixed_window", mapOf("windowSizeMs" to "100"))
        val validator = OperationalValidator()
        val result = validator.validate(action)
        
        assertFalse(result.isValid)
        assertTrue(result.message.contains("Rejected"))
    }

    @Test
    fun `ControlPlane should throw exception on rejected config in Operational mode`() {
        val law = ControlLaw(listOf(
            Policy(name = "unsafe", action = Action("fixed_window", mapOf("windowSizeMs" to "100")))
        ))
        val router = PolicyRouter(law)
        val factory = DefaultMechanismFactory()
        val controlPlane = ControlPlane(router, factory, OperationalValidator())

        val context = RequestContext(key = "user1")
        assertThrows(InstabilityRiskException::class.java) {
            controlPlane.execute(context)
        }
    }
}
