package com.urlshortener.event

import java.time.Instant

data class UrlAccessedEvent(
    val shortCode: String,
    val accessedAt: Instant = Instant.now()
)
