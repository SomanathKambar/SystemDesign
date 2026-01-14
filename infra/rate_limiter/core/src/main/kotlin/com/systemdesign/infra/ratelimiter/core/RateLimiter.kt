package com.systemdesign.infra.ratelimiter.core

import com.systemdesign.infra.ratelimiter.core.model.Decision

interface RateLimiter {
    /**
     * Checks if a request for the given key is allowed.
     */
    fun allow(key: String): Decision
}
