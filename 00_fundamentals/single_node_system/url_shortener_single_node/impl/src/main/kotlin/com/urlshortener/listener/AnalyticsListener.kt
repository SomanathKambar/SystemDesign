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
        // In a real system, we might push this to a queue (Kafka/RabbitMQ)
        // For now, we update the DB directly but separately from the main flow logic
        val entity = repo.findByShortCode(event.shortCode)
        if (entity != null) {
            entity.lastAccessedAt = event.accessedAt
            entity.clickCount++
            repo.save(entity)
        }
    }

    @EventListener
    @Transactional
    fun onUrlCreationRequested(event: UrlCreationRequestedEvent) {
        // Track how many times people ask for the same URL
        val entity = repo.findByUrlHash(event.urlHash)
        if (entity != null) {
            entity.creationRequestCount++
            repo.save(entity)
        }
    }
}
