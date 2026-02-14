package com.systemdesign.infra.ratelimiter.engine.governance

import com.systemdesign.infra.ratelimiter.core.dsl.Action

enum class Profile {
    SIMULATION, OPERATIONAL
}

data class ValidationResult(
    val isValid: Boolean,
    val message: String,
    val isWarning: Boolean = false
)

interface CapabilityValidator {
    fun validate(action: Action): ValidationResult
}

class InstabilityRiskException(message: String) : RuntimeException(message)
