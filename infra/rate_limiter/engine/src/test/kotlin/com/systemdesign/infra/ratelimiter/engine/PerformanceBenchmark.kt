package com.systemdesign.infra.ratelimiter.engine

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.system.measureTimeMillis

class PerformanceBenchmark {

    @Test
    fun `benchmark decision client overhead`() {
        val policyJson = """
        {
            "policies": [
                {
                    "name": "benchmark",
                    "then": {
                        "use": "token_bucket",
                        "params": {
                            "capacity": "1000",
                            "refillRate": "100"
                        }
                    }
                }
            ]
        }
        """.trimIndent()

        val client = DecisionClient.builder()
            .withPolicyJson(policyJson)
            .withOperationalMode()
            .build()

        // Warmup
        repeat(1000) {
            client.evaluate("warmup")
        }

        val iterations = 10000
        val totalTime = measureTimeMillis {
            repeat(iterations) {
                client.evaluate("key-$it")
            }
        }

        val avgTime = totalTime.toDouble() / iterations
        println("Average decision time: ${avgTime}ms over $iterations iterations")
        
        // Target: < 1ms for in-memory
        assertTrue(avgTime < 1.0, "Decision time should be less than 1ms, got ${avgTime}ms")
    }
}
