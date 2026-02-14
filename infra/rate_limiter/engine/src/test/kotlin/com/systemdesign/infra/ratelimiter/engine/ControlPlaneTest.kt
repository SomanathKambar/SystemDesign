package com.systemdesign.infra.ratelimiter.engine

import com.systemdesign.infra.ratelimiter.core.dsl.Action
import com.systemdesign.infra.ratelimiter.core.dsl.ControlLaw
import com.systemdesign.infra.ratelimiter.core.dsl.Policy
import com.systemdesign.infra.ratelimiter.core.dsl.PolicyRouter
import com.systemdesign.infra.ratelimiter.core.model.RequestContext
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ControlPlaneTest {

    @Test
    fun `should execute matched mechanism`() {
        val law = ControlLaw(listOf(
            Policy(name = "default", action = Action("fixed_window", mapOf("limit" to "10", "windowSizeMs" to "1000")))
        ))
        val router = PolicyRouter(law)
        val factory = DefaultMechanismFactory()
        val controlPlane = ControlPlane(router, factory)

        val context = RequestContext(key = "user1")
        val decision = controlPlane.execute(context)
        
        assertTrue(decision.allowed)
        assertTrue(decision.reason.contains("Allowed"))
    }
}
