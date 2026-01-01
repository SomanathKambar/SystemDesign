package com.urlshortener.event

data class UrlCreationRequestedEvent(
    val urlHash: String
)
