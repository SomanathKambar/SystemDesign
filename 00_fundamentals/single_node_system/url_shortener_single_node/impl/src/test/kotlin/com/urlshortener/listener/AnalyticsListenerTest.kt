package com.urlshortener.listener

import com.urlshortener.event.UrlAccessedEvent
import com.urlshortener.event.UrlCreationRequestedEvent
import com.urlshortener.model.UrlEntity
import com.urlshortener.repository.UrlRepository
import io.mockk.*
import org.junit.jupiter.api.Test
import java.time.Instant

class AnalyticsListenerTest {

    private val repository: UrlRepository = mockk(relaxed = true)
    private val listener = AnalyticsListener(repository)

    @Test
    fun `should call atomic increment for click count on UrlAccessedEvent`() {
        // Given
        val shortCode = "ABC12345"
        val event = UrlAccessedEvent(shortCode = shortCode, accessedAt = Instant.now())
        
        every { repository.incrementClickCount(any(), any()) } just runs

        // When
        listener.onUrlAccessed(event)

        // Then
        verify(exactly = 1) { repository.incrementClickCount(shortCode, event.accessedAt) }
    }

    @Test
    fun `should call atomic increment for creation request on UrlCreationRequestedEvent`() {
        // Given
        val hash = "someHash"
        val event = UrlCreationRequestedEvent(urlHash = hash)
        
        every { repository.incrementCreationRequestCount(any()) } just runs

        // When
        listener.onUrlCreationRequested(event)

        // Then
        verify(exactly = 1) { repository.incrementCreationRequestCount(hash) }
    }
}
