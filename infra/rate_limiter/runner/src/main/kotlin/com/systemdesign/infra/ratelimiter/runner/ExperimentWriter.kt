package com.systemdesign.infra.ratelimiter.runner

import com.systemdesign.infra.ratelimiter.core.event.RateLimitEvent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.*

class ExperimentWriter(private val baseDir: String = "experiments") {
    private val json = Json { prettyPrint = true; encodeDefaults = true }
    private val jsonl = Json { encodeDefaults = true }

    fun write(
        metadata: ExperimentMetadata,
        events: List<RateLimitEvent>
    ): File {
        val experimentDir = File(baseDir, "${metadata.strategy.lowercase()}/${metadata.id}")
        experimentDir.mkdirs()

        // Write metadata
        val metaFile = File(experimentDir, "metadata.json")
        metaFile.writeText(json.encodeToString(metadata))

        // Write events
        val eventsFile = File(experimentDir, "events.jsonl")
        eventsFile.printWriter().use { out ->
            events.forEach { event ->
                out.println(jsonl.encodeToString(event))
            }
        }

        return experimentDir
    }
}
