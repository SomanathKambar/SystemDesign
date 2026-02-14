package com.systemdesign.infra.ratelimiter.core.dsl

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PolicyLoaderTest {

    @Test
    fun `should load policies from json`() {
        val json = """
            {
                "policies": [
                    {
                        "name": "spike-protection",
                        "priority": 100,
                        "when": {
                            "metric": "rps",
                            "operator": ">",
                            "value": 1000.0
                        },
                        "then": {
                            "use": "shed_load",
                            "params": {
                                "mode": "aggressive"
                            }
                        }
                    }
                ]
            }
        """.trimIndent()

        val law = PolicyLoader.loadFromJson(json)
        assertEquals(1, law.policies.size)
        assertEquals("spike-protection", law.policies[0].name)
        assertEquals(100, law.policies[0].priority)
        assertEquals("rps", law.policies[0].condition?.metric)
        assertEquals("shed_load", law.policies[0].action.use)
        assertEquals("aggressive", law.policies[0].action.params["mode"])
    }
}
