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
}
