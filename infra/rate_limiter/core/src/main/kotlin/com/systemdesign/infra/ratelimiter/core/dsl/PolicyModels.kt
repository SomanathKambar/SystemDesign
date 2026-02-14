package com.systemdesign.infra.ratelimiter.core.dsl

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ControlLaw(
    val policies: List<Policy>
)

@Serializable
data class Policy(
    val name: String,
    val priority: Int = 0,
    @SerialName("when")
    val condition: Condition? = null,
    @SerialName("then")
    val action: Action
)

@Serializable
data class Condition(
    val all: List<Condition>? = null,
    val any: List<Condition>? = null,
    val not: Condition? = null,
    val metric: String? = null,
    val operator: String? = null,
    val value: Double? = null
)

@Serializable
data class Action(
    val use: String, // Mechanism name: "fixed_window", "token_bucket", etc.
    val params: Map<String, String> = emptyMap()
)
