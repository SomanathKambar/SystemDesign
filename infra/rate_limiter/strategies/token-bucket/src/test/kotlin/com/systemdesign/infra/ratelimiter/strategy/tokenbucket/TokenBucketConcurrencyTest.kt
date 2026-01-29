package com.systemdesign.infra.ratelimiter.strategy.tokenbucket

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class TokenBucketConcurrencyTest {

    @Test
    fun `should handle concurrent requests correctly`() {
        val capacity = 100.0
        val limiter = TokenBucketRateLimiter(
            capacity = capacity,
            refillTokensPerSecond = 1.0 // Slow refill, so we rely on initial capacity
        )

        val threads = 20
        val requestsPerThread = 10
        val executor = Executors.newFixedThreadPool(threads)
        val latch = CountDownLatch(1)
        val successCount = AtomicInteger(0)

        // Total requests = 200. Capacity = 100.
        // We expect exactly 100 successes if thread-safe.
        // If race condition exists, we might get more than 100.

        for (i in 0 until threads) {
            executor.submit {
                latch.await() // Wait for start signal
                for (j in 0 until requestsPerThread) {
                    if (limiter.allow("concurrent-user").allowed) {
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
        // In a race condition, reading the same state allows multiple threads to succeed 
        // thinking they are consuming the same token.
        // So successCount would be > 100.
        
        // However, asserting strictly <= 100 might fail if the implementation is broken.
        // Ideally we want exactly 100 because we are blasting them at once and capacity is 100.
        // But if refill happens (very slow here), it might be slightly more.
        // With 1.0 refill/sec, in the few ms this test runs, refill is negligible (< 1 token).
        
        assertTrue(successCount.get() <= 100, "Expected <= 100 successful requests, but got ${successCount.get()}")
    }
}
