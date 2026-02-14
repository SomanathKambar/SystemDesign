package com.systemdesign.infra.ratelimiter.core

import com.systemdesign.infra.ratelimiter.core.model.Decision
import com.systemdesign.infra.ratelimiter.core.model.RequestContext

class MechanismAdapter(private val mechanism: Mechanism) : RateLimiter {
    override fun allow(key: String): Decision {
        return mechanism.execute(RequestContext(key = key))
    }
}
