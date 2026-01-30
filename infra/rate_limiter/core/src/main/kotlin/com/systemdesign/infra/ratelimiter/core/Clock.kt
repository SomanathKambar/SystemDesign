package com.systemdesign.infra.ratelimiter.core

/**
 * Interface for providing time, allowing for deterministic simulations.
 */
interface Clock {
    /**
     * Returns the current time in milliseconds.
     */
    fun currentTimeMillis(): Long
}

/**
 * Standard implementation using the system clock.
 */
class SystemClock : Clock {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}

/**
 * A clock implementation that allows manual control of time for testing and simulations.
 */
class TestClock(initialMillis: Long = 0) : Clock {
    private var currentTime = initialMillis

    override fun currentTimeMillis(): Long = currentTime

    fun advanceBy(millis: Long) {
        currentTime += millis
    }

    fun setTime(millis: Long) {
        currentTime = millis
    }
}
