package com.urlshortener.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class UrlShortenerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `smoke test - full flow`() {
        val longUrl = "https://integration-test.com"
        val request = mapOf("long_url" to longUrl)

        // 1. Shorten
        val result = mockMvc.perform(post("/shorten")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk)
            .andReturn()

        val responseContent = result.response.contentAsString
        val responseMap = objectMapper.readValue(responseContent, Map::class.java)
        val shortUrl = responseMap["short_url"] as String
        val shortCode = shortUrl.substringAfterLast("/")

        assertTrue(shortCode.length == 8)

        // 2. Resolve
        mockMvc.perform(get("/$shortCode"))
            .andExpect(status().isFound)
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Location", longUrl))
    }
}
