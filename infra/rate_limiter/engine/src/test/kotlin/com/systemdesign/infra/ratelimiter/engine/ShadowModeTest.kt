package com.systemdesign.infra.ratelimiter.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ShadowModeTest {

    @Test
    fun `Shadow mode should always allow but track original decision`() {
        val policyJson = """
            {
                "policies": [
                    {
                        "name": "strict",
                        "priority": 0,
                        "then": {
                            "use": "fixed_window",
                            "params": {
                                "limit": "1",
                                "windowSizeMs": "1000"
                            }
                        }
                    }
                ]
            }
        """.trimIndent()

        val client = DecisionClient.builder()
            .withPolicyJson(policyJson)
            .withOperationalMode()
            .withShadowMode()
            .build()

        // 1st request
        val d1 = client.evaluate("u1")
        assertTrue(d1.allowed)
        assertEquals(true, d1.metadata["shadow_original_allowed"])

        // 2nd request - should be blocked by fixed window logic, but allowed by shadow mode
        val d2 = client.evaluate("u1")
        assertTrue(d2.allowed)
        assertEquals(false, d2.metadata["shadow_original_allowed"])
        assertTrue(d2.metadata.containsKey("shadow_mode"))
    }
}
