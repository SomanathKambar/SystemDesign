package com.systemdesign.infra.ratelimiter.core

import com.systemdesign.infra.ratelimiter.core.model.Decision
import com.systemdesign.infra.ratelimiter.core.model.RequestContext

/**
 * A Mechanism is a deterministic, stateless execution unit that applies a Control Law (Policy).
 * Examples: FixedWindowMechanism, TokenBucketMechanism, CircuitBreakerMechanism.
 */
interface Mechanism {
    /**
     * Executes the mechanism against the given context.
     */
    fun execute(context: RequestContext): Decision
}
