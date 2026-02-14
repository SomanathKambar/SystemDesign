package com.systemdesign.infra.ratelimiter.core.dsl

import com.systemdesign.infra.ratelimiter.core.model.RequestContext
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PolicyRouterTest {

    @Test
    fun `should match policy based on priority`() {
        val law = ControlLaw(listOf(
            Policy(name = "low", priority = 10, action = Action("fixed")),
            Policy(name = "high", priority = 100, action = Action("token"))
        ))
        val router = PolicyRouter(law)
        val match = router.match(RequestContext(key = "test"))
        assertEquals("high", match?.name)
    }

    @Test
    fun `should match based on metrics`() {
        val law = ControlLaw(listOf(
            Policy(
                name = "spike",
                priority = 100,
                condition = Condition(metric = "rps", operator = ">", value = 100.0),
                action = Action("shed")
            ),
            Policy(
                name = "normal",
                priority = 10,
                action = Action("fixed")
            )
        ))
        val router = PolicyRouter(law)

        // Normal traffic
        val normalContext = RequestContext(key = "test", metrics = mapOf("rps" to 50.0))
        assertEquals("normal", router.match(normalContext)?.name)

        // Spike traffic
        val spikeContext = RequestContext(key = "test", metrics = mapOf("rps" to 150.0))
        assertEquals("spike", router.match(spikeContext)?.name)
    }
}
