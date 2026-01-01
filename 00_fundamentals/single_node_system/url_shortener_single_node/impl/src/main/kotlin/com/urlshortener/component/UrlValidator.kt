package com.urlshortener.component

import org.springframework.stereotype.Component
import java.net.URI

@Component
class UrlValidator {
    
    companion object {
        private const val MAX_LENGTH = 2048
        private val ALLOWED_PROTOCOLS = listOf("http", "https")
    }

    /**
     * Validates and normalizes a given URL.
     * Throws IllegalArgumentException if invalid.
     * Returns the normalized URL string.
     */
    fun validateAndNormalize(url: String): String {
        val trimmed = url.trim()
        
        if (trimmed.isEmpty()) {
            throw IllegalArgumentException("URL cannot be empty")
        }
        
        if (trimmed.length > MAX_LENGTH) {
            throw IllegalArgumentException("URL is too long (max $MAX_LENGTH characters)")
        }

        return try {
            val uri = URI(trimmed)
            if (uri.scheme == null || !ALLOWED_PROTOCOLS.contains(uri.scheme.lowercase())) {
                throw IllegalArgumentException("Only http and https protocols are supported")
            }
            // Normalization: remove trailing slash for consistency
            trimmed.removeSuffix("/")
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid URL format: ${e.message}")
        }
    }
}
