package com.systemdesign.infra.ratelimiter.engine.governance

import com.systemdesign.infra.ratelimiter.core.dsl.Action

object SafeOperatingEnvelope {
    const val FIXED_WINDOW_MIN_SIZE_MS = 1000L
    const val TOKEN_BUCKET_MIN_REFILL_RATE = 0.1
    const val LEAKY_BUCKET_MIN_LEAK_RATE = 0.1
}

class SimulationValidator : CapabilityValidator {
    override fun validate(action: Action): ValidationResult {
        return when (action.use.lowercase()) {
            "fixed_window" -> {
                val windowSize = action.params["windowSizeMs"]?.toLong() ?: 1000L
                if (windowSize < SafeOperatingEnvelope.FIXED_WINDOW_MIN_SIZE_MS) {
                    ValidationResult(true, "Warning: Window size ${windowSize}ms is below recommended 1s. High burst risk.", isWarning = true)
                } else {
                    ValidationResult(true, "OK")
                }
            }
            "token_bucket" -> {
                val refill = action.params["refillRate"]?.toDouble() ?: 1.0
                if (refill < SafeOperatingEnvelope.TOKEN_BUCKET_MIN_REFILL_RATE) {
                    ValidationResult(true, "Warning: Very low refill rate ($refill) might lead to stagnation.", isWarning = true)
                } else {
                    ValidationResult(true, "OK")
                }
            }
            else -> ValidationResult(true, "OK")
        }
    }
}

class OperationalValidator : CapabilityValidator {
    override fun validate(action: Action): ValidationResult {
        return when (action.use.lowercase()) {
            "fixed_window" -> {
                val windowSize = action.params["windowSizeMs"]?.toLong() ?: 1000L
                if (windowSize < SafeOperatingEnvelope.FIXED_WINDOW_MIN_SIZE_MS) {
                    ValidationResult(false, "Rejected: Window size ${windowSize}ms is below safe minimum of ${SafeOperatingEnvelope.FIXED_WINDOW_MIN_SIZE_MS}ms.")
                } else {
                    ValidationResult(true, "OK")
                }
            }
            "token_bucket" -> {
                val refill = action.params["refillRate"]?.toDouble() ?: 1.0
                if (refill < SafeOperatingEnvelope.TOKEN_BUCKET_MIN_REFILL_RATE) {
                    ValidationResult(false, "Rejected: Refill rate $refill is below safe minimum of ${SafeOperatingEnvelope.TOKEN_BUCKET_MIN_REFILL_RATE}.")
                } else {
                    ValidationResult(true, "OK")
                }
            }
            else -> ValidationResult(true, "OK")
        }
    }
}
