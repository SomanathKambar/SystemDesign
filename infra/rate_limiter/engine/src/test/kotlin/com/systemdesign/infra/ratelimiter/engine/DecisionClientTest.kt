package com.systemdesign.infra.ratelimiter.engine

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DecisionClientTest {

    @Test
    fun `should build and use client with fluent API`() {
        val policyJson = """
            {
                "policies": [
                    {
                        "name": "default",
                        "priority": 0,
                        "then": {
                            "use": "fixed_window",
                            "params": {
                                "limit": "5",
                                "windowSizeMs": "1000"
                            }
                        }
                    }
                ]
            }
        """.trimIndent()

        val client = DecisionClient.builder()
            .withPolicyJson(policyJson)
            .withSimulationMode()
            .build()

        val decision = client.evaluate("user123")
        assertTrue(decision.allowed)
    }
}
