package com.systemdesign.infra.ratelimiter.core.dsl

import com.systemdesign.infra.ratelimiter.core.model.RequestContext

class PolicyRouter(private val controlLaw: ControlLaw) {
    
    /**
     * Matches the request context against the defined policies and returns the one with the highest priority.
     */
    fun match(context: RequestContext): Policy? {
        return controlLaw.policies
            .sortedByDescending { it.priority }
            .firstOrNull { it.condition == null || evaluate(it.condition, context) }
    }

    private fun evaluate(condition: Condition, context: RequestContext): Boolean {
        // Logical AND
        if (condition.all != null) {
            return condition.all.all { evaluate(it, context) }
        }
        
        // Logical OR
        if (condition.any != null) {
            return condition.any.any { evaluate(it, context) }
        }
        
        // Logical NOT
        if (condition.not != null) {
            return !evaluate(condition.not, context)
        }
        
        // Metric Comparison
        if (condition.metric != null) {
            val metricValue = context.metrics[condition.metric] ?: 0.0
            val targetValue = condition.value ?: 0.0
            return when (condition.operator) {
                ">" -> metricValue > targetValue
                "<" -> metricValue < targetValue
                ">=" -> metricValue >= targetValue
                "<=" -> metricValue <= targetValue
                "==" -> metricValue == targetValue
                "!=" -> metricValue != targetValue
                else -> false
            }
        }
        
        // Empty condition defaults to true
        return true
    }
}
