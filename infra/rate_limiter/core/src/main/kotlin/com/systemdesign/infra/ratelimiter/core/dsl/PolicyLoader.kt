package com.systemdesign.infra.ratelimiter.core.dsl

import kotlinx.serialization.json.Json

object PolicyLoader {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    /**
     * Loads a ControlLaw from a JSON string.
     */
    fun loadFromJson(jsonString: String): ControlLaw {
        return json.decodeFromString<ControlLaw>(jsonString)
    }
}
