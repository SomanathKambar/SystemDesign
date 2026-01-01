package com.urlshortener.service

import com.urlshortener.component.ShortCodeGenerator
import com.urlshortener.component.UrlValidator
import com.urlshortener.event.UrlAccessedEvent
import com.urlshortener.event.UrlCreationRequestedEvent
import com.urlshortener.model.UrlEntity
import com.urlshortener.repository.UrlRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.Instant

@Service
class UrlService(
    private val repo: UrlRepository,
    private val validator: UrlValidator,
    private val codeGenerator: ShortCodeGenerator,
    private val eventPublisher: ApplicationEventPublisher
) {

    @Transactional
    fun shorten(longUrl: String): String {
        // 1. Validate & Normalize
        val normalizedUrl = validator.validateAndNormalize(longUrl)
        val hash = hashUrl(normalizedUrl)

        // 2. Publish "Intent" Event (Analytics)
        // We track that someone WANTED this URL, regardless of whether it's new or old.
        eventPublisher.publishEvent(UrlCreationRequestedEvent(hash))

        // 3. Deduplication
        repo.findByUrlHash(hash)?.let { return it.shortCode }

        // 4. Generation loop
        repeat(5) { 
            val code = codeGenerator.generate()
            try {
                repo.save(UrlEntity(
                    shortCode = code,
                    longUrl = normalizedUrl,
                    urlHash = hash
                ))
                return code
            } catch (e: DataIntegrityViolationException) {
                repo.findByUrlHash(hash)?.let { return it.shortCode }
            }
        }
        throw RuntimeException("Failed to generate unique code after multiple attempts")
    }

    @Transactional
    fun resolve(code: String): String? {
        val entity = repo.findByShortCode(code) ?: return null
        
        // Fire and Forget (Analytics)
        // The service doesn't care *how* we track clicks, just that a click happened.
        eventPublisher.publishEvent(UrlAccessedEvent(code))
        
        return entity.longUrl
    }

    private fun hashUrl(url: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(url.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
