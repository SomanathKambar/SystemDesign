package com.urlshortener.service

import com.urlshortener.model.UrlEntity
import com.urlshortener.repository.UrlRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UrlServiceTest {

    private val repository: UrlRepository = mockk()
    private val service = UrlService(repository)

    @Test
    fun `should shorten url successfully`() {
        // Given
        val longUrl = "https://google.com"
        every { repository.findByShortCode(any()) } returns null
        every { repository.save(any()) } returnsArgument 0

        // When
        val shortCode = service.shorten(longUrl)

        // Then
        assertNotNull(shortCode)
        assertEquals(8, shortCode.length)
        verify(exactly = 1) { repository.save(any()) }
    }

    @Test
    fun `should resolve url successfully`() {
        // Given
        val shortCode = "ABC12345"
        val longUrl = "https://google.com"
        every { repository.findByShortCode(shortCode) } returns UrlEntity(shortCode = shortCode, longUrl = longUrl)

        // When
        val result = service.resolve(shortCode)

        // Then
        assertEquals(longUrl, result)
    }

    @Test
    fun `should return null when resolving unknown code`() {
        // Given
        every { repository.findByShortCode("unknown") } returns null

        // When
        val result = service.resolve("unknown")

        // Then
        assertEquals(null, result)
    }
}
