package com.systemdesign.infra.ratelimiter.strategy.tokenbucket

import com.systemdesign.infra.ratelimiter.core.model.RequestContext
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class TokenBucketConcurrencyMechanismTest {

    @Test
    fun `should handle concurrent requests correctly`() {
        val capacity = 100.0
        val limiter = TokenBucketMechanism(
            capacity = capacity,
            refillTokensPerSecond = 1.0 // Slow refill, so we rely on initial capacity
        )

        val threads = 20
        val requestsPerThread = 10
        val executor = Executors.newFixedThreadPool(threads)
        val latch = CountDownLatch(1)
        val successCount = AtomicInteger(0)

        // Total requests = 200. Capacity = 100.
        
        val context = RequestContext(key = "concurrent-user")

        for (i in 0 until threads) {
            executor.submit {
                latch.await() // Wait for start signal
                for (j in 0 until requestsPerThread) {
                    if (limiter.execute(context).allowed) {
                        successCount.incrementAndGet()
                    }
                }
            }
        }

        latch.countDown() // Start all threads
        executor.shutdown()
        while (!executor.isTerminated) {
            Thread.sleep(10)
        }

        println("Success count: ${successCount.get()}")
        assertTrue(successCount.get() <= 100, "Expected <= 100 successful requests, but got ${successCount.get()}")
    }
}