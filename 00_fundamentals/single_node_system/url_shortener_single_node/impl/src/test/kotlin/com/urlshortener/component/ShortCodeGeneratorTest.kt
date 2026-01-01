package com.urlshortener.component

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ShortCodeGeneratorTest {

    private val generator = RandomShortCodeGenerator()

    @Test
    fun `should generate code with correct length`() {
        val code = generator.generate()
        assertEquals(8, code.length)
    }

    @Test
    fun `should generate alphanumeric codes`() {
        val code = generator.generate()
        val regex = Regex("^[a-zA-Z0-9]+$")
        assertTrue(regex.matches(code), "Code $code should be alphanumeric")
    }

    @Test
    fun `should generate unique codes on multiple calls`() {
        val codes = (1..100).map { generator.generate() }.toSet()
        assertEquals(100, codes.size, "Should have generated 100 unique codes")
    }
}
