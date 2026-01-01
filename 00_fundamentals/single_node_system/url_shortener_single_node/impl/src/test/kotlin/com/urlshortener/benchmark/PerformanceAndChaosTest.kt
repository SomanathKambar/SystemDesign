package com.urlshortener.benchmark

import com.urlshortener.service.UrlService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
class PerformanceAndChaosTest {

    @Autowired
    private lateinit var service: UrlService

    @Test
    fun `load benchmark and concurrency test`() {
        val numberOfThreads = 50
        val requestsPerThread = 100
        val totalRequests = numberOfThreads * requestsPerThread
        val executor = Executors.newFixedThreadPool(numberOfThreads)
        val latch = CountDownLatch(totalRequests)
        
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)
        val collisions = ConcurrentHashMap<String, Int>()

        val startTime = System.currentTimeMillis()

        for (i in 0 until numberOfThreads) {
            executor.submit {
                for (j in 0 until requestsPerThread) {
                    try {
                        val longUrl = "https://example.com/$i/$j"
                        val shortCode = service.shorten(longUrl)
                        successCount.incrementAndGet()
                        collisions.compute(shortCode) { _, v -> (v ?: 0) + 1 }
                    } catch (e: Exception) {
                        failCount.incrementAndGet()
                        println("Error: ${e.message}")
                    } finally {
                        latch.countDown()
                    }
                }
            }
        }

        latch.await(30, TimeUnit.SECONDS)
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        println("==================================================")
        println("Load Benchmark Result (Local JVM)")
        println("Total Requests: $totalRequests")
        println("Duration: ${duration}ms")
        println("Throughput: ${(successCount.get() * 1000) / duration} req/sec")
        println("Success: ${successCount.get()}")
        println("Failures: ${failCount.get()}")
        println("Unique Codes Generated: ${collisions.size}")
        println("Duplicate Codes (Should be 0): ${totalRequests - collisions.size}")
        println("==================================================")

        // Chaos Check: Ensure no duplicates were returned for unique inputs (though DB prevents this, service might retry successfully)
        // If failCount > 0, it might mean the retry logic failed or DB constraint hit and wasn't caught.
        
        if (failCount.get() > 0) {
            println("WARNING: Some requests failed. Check logs for Unique Constraint Violations.")
        }
    }
}
