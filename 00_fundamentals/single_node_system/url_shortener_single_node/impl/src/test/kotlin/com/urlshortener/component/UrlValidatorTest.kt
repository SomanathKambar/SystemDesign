package com.urlshortener.component

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UrlValidatorTest {

    private val validator = UrlValidator()

    @Test
    fun `should validate and normalize valid urls`() {
        assertEquals("https://google.com", validator.validateAndNormalize("https://google.com"))
        assertEquals("https://google.com", validator.validateAndNormalize("https://google.com/"))
        assertEquals("http://example.org/path", validator.validateAndNormalize("  http://example.org/path  "))
    }

    @Test
    fun `should throw error for empty urls`() {
        assertThrows<IllegalArgumentException> { validator.validateAndNormalize("") }
        assertThrows<IllegalArgumentException> { validator.validateAndNormalize("   ") }
    }

    @Test
    fun `should throw error for too long urls`() {
        val longUrl = "https://example.com/" + "a".repeat(2050)
        assertThrows<IllegalArgumentException> { validator.validateAndNormalize(longUrl) }
    }

    @Test
    fun `should throw error for invalid protocols`() {
        assertThrows<IllegalArgumentException> { validator.validateAndNormalize("ftp://google.com") }
        assertThrows<IllegalArgumentException> { validator.validateAndNormalize("mailto:test@example.com") }
    }

    @Test
    fun `should throw error for malformed urls`() {
        assertThrows<IllegalArgumentException> { validator.validateAndNormalize("not-a-url") }
        assertThrows<IllegalArgumentException> { validator.validateAndNormalize("http://") }
    }
}
