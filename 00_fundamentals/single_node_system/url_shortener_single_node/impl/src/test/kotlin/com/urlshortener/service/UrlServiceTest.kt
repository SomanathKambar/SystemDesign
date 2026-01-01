package com.urlshortener.service

import com.urlshortener.component.ShortCodeGenerator
import com.urlshortener.component.UrlValidator
import com.urlshortener.model.UrlEntity
import com.urlshortener.repository.UrlRepository
import io.mockk.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.context.ApplicationEventPublisher

class UrlServiceTest {

    private val repository: UrlRepository = mockk()
    private val validator: UrlValidator = mockk()
    private val generator: ShortCodeGenerator = mockk()
    private val eventPublisher: ApplicationEventPublisher = mockk(relaxed = true)
    
    private val service = UrlService(repository, validator, generator, eventPublisher)

    @Test
    fun `should shorten url successfully`() {
        // Given
        val longUrl = "https://google.com"
        every { validator.validateAndNormalize(longUrl) } returns longUrl
        every { generator.generate() } returns "ABC12345"
        every { repository.findByUrlHash(any()) } returns null
        every { repository.save(any()) } returnsArgument 0
        
        // When
        val shortCode = service.shorten(longUrl)

        // Then
        assertNotNull(shortCode)
        assertEquals("ABC12345", shortCode)
        verify(exactly = 1) { repository.save(any()) }
        
        val slot = slot<Any>()
        verify { eventPublisher.publishEvent(capture(slot)) }
        println("Captured event: ${slot.captured}")
    }

    @Test
    fun `should deduplicate url`() {
        // Given
        val longUrl = "https://google.com"
        val existingCode = "ABC12345"
        val existingEntity = UrlEntity(shortCode = existingCode, longUrl = longUrl, urlHash = "somehash")
        
        every { validator.validateAndNormalize(longUrl) } returns longUrl
        every { repository.findByUrlHash(any()) } returns existingEntity

        // When
        val shortCode = service.shorten(longUrl)

        // Then
        assertEquals(existingCode, shortCode)
        verify(exactly = 0) { repository.save(any()) }
        
        val slot = slot<Any>()
        verify { eventPublisher.publishEvent(capture(slot)) }
    }

    @Test
    fun `should propagate validation error`() {
        every { validator.validateAndNormalize(any()) } throws IllegalArgumentException("Invalid URL")
        
        assertThrows<IllegalArgumentException> {
            service.shorten("invalid")
        }
    }

    @Test
    fun `should resolve url successfully and publish event`() {
        // Given
        val shortCode = "ABC12345"
        val longUrl = "https://google.com"
        val entity = UrlEntity(shortCode = shortCode, longUrl = longUrl, urlHash = "hash")
        every { repository.findByShortCode(shortCode) } returns entity

        // When
        val result = service.resolve(shortCode)

        // Then
        assertEquals(longUrl, result)
        verify(exactly = 0) { repository.save(any()) }
        
        val slot = slot<Any>()
        verify { eventPublisher.publishEvent(capture(slot)) }
    }
}
