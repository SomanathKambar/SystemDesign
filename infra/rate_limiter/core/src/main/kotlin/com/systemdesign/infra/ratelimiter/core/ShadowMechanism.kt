package com.systemdesign.infra.ratelimiter.core

import com.systemdesign.infra.ratelimiter.core.model.Decision
import com.systemdesign.infra.ratelimiter.core.model.RequestContext

/**
 * A Mechanism decorator that runs the original mechanism but always returns 'allowed = true'.
 * Useful for testing new policies in production without affecting traffic.
 */
class ShadowMechanism(private val delegate: Mechanism) : Mechanism {
    
    override fun execute(context: RequestContext): Decision {
        val originalDecision = delegate.execute(context)
        
        return originalDecision.copy(
            allowed = true,
            metadata = originalDecision.metadata + mapOf(
                "shadow_mode" to true,
                "shadow_original_allowed" to originalDecision.allowed,
                "shadow_original_reason" to originalDecision.reason
            )
        )
    }
}
