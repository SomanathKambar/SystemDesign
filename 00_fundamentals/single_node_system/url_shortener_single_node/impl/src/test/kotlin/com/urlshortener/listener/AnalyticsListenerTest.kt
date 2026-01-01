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
    fun `should update lastAccessedAt and increment clickCount on UrlAccessedEvent`() {
        // Given
        val shortCode = "ABC12345"
        val event = UrlAccessedEvent(shortCode = shortCode, accessedAt = Instant.now())
        val entity = UrlEntity(
            shortCode = shortCode,
            longUrl = "http://example.com",
            urlHash = "hash",
            clickCount = 10
        )

        every { repository.findByShortCode(shortCode) } returns entity
        every { repository.save(any<UrlEntity>()) } returnsArgument 0

        // When
        listener.onUrlAccessed(event)

        // Then
        verify(exactly = 1) { repository.save(any<UrlEntity>()) }
        assert(entity.clickCount == 11L)
        assert(entity.lastAccessedAt == event.accessedAt)
    }

    @Test
    fun `should increment creationRequestCount on UrlCreationRequestedEvent`() {
        // Given
        val hash = "someHash"
        val event = UrlCreationRequestedEvent(urlHash = hash)
        val entity = UrlEntity(
            shortCode = "ABC",
            longUrl = "http://example.com",
            urlHash = hash,
            creationRequestCount = 5
        )

        every { repository.findByUrlHash(hash) } returns entity
        every { repository.save(any<UrlEntity>()) } returnsArgument 0

        // When
        listener.onUrlCreationRequested(event)

        // Then
        verify(exactly = 1) { repository.save(any<UrlEntity>()) }
        assert(entity.creationRequestCount == 6L)
    }
}
