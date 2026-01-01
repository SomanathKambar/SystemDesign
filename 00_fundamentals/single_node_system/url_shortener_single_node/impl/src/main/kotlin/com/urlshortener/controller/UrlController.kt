package com.urlshortener.controller

import com.urlshortener.service.UrlService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
class UrlController(private val service: UrlService) {

    @PostMapping("/shorten")
    fun shorten(@RequestBody req: Map<String, String>): Map<String, String> {
        val longUrl = req["long_url"] ?: throw IllegalArgumentException("Missing 'long_url' in request body")
        return mapOf("short_url" to "http://localhost:8080/" + service.shorten(longUrl))
    }

    @GetMapping("/{code}")
    fun redirect(@PathVariable code: String): ResponseEntity<Void> {
        val url = service.resolve(code) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.status(302).location(URI.create(url)).build()
    }

    @GetMapping("/stats/{code}")
    fun getStats(@PathVariable code: String): ResponseEntity<Map<String, Any>> {
        val stats = service.getStats(code) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(mapOf(
            "short_code" to stats.shortCode,
            "long_url" to stats.longUrl,
            "click_count" to stats.clickCount,
            "creation_request_count" to stats.creationRequestCount,
            "created_at" to stats.createdAt,
            "last_accessed_at" to stats.lastAccessedAt
        ))
    }

    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, String>> = 
        ResponseEntity.ok(mapOf("status" to "UP"))
}
