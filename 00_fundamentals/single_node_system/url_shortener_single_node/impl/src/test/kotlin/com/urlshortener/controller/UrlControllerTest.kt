package com.urlshortener.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.urlshortener.service.UrlService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(UrlController::class)
class UrlControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var service: UrlService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should shorten url`() {
        val longUrl = "https://example.com"
        val shortCode = "ABC12345"
        
        every { service.shorten(longUrl) } returns shortCode

        val request = mapOf("long_url" to longUrl)

        mockMvc.perform(post("/shorten")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.short_url").value("http://localhost:8080/$shortCode"))
    }

    @Test
    fun `should redirect to original url`() {
        val shortCode = "ABC12345"
        val longUrl = "https://example.com"

        every { service.resolve(shortCode) } returns longUrl

        mockMvc.perform(get("/$shortCode"))
            .andExpect(status().isFound)
            .andExpect(header().string("Location", longUrl))
    }

    @Test
    fun `should return 404 when url not found`() {
        every { service.resolve("unknown") } returns null

        mockMvc.perform(get("/unknown"))
            .andExpect(status().isNotFound)
    }
}
