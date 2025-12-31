package com.urlshortener.service

import com.urlshortener.model.UrlEntity
import com.urlshortener.repository.UrlRepository
import org.springframework.stereotype.Service

@Service
class UrlService(private val repo: UrlRepository) {

    fun shorten(longUrl: String): String {
        while (true) {
            val code = generateCode()
            if (repo.findByShortCode(code) == null) {
                repo.save(UrlEntity(shortCode = code, longUrl = longUrl))
                return code
            }
        }
    }

    fun resolve(code: String): String? =
        repo.findByShortCode(code)?.longUrl

    private fun generateCode() =
        (1..8).map { "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".random() }.joinToString("")
}
