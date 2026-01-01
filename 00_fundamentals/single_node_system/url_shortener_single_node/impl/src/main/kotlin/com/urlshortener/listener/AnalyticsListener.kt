package com.urlshortener.listener

import com.urlshortener.event.UrlAccessedEvent
import com.urlshortener.event.UrlCreationRequestedEvent
import com.urlshortener.repository.UrlRepository
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AnalyticsListener(private val repo: UrlRepository) {

    @EventListener
    @Transactional
    fun onUrlAccessed(event: UrlAccessedEvent) {
        // Atomic increment in DB prevents "Lost Updates" from concurrent threads
        repo.incrementClickCount(event.shortCode, event.accessedAt)
    }

    @EventListener
    @Transactional
    fun onUrlCreationRequested(event: UrlCreationRequestedEvent) {
        // Atomic increment in DB ensures we count every attempt correctly
        repo.incrementCreationRequestCount(event.urlHash)
    }
}
