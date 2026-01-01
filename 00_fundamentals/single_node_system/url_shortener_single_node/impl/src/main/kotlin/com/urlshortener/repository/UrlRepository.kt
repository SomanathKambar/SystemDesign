package com.urlshortener.repository

import com.urlshortener.model.UrlEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.Instant

interface UrlRepository : JpaRepository<UrlEntity, Long> {
    fun findByShortCode(code: String): UrlEntity?
    fun findByUrlHash(hash: String): UrlEntity?

    @Modifying
    @Query("UPDATE UrlEntity u SET u.clickCount = u.clickCount + 1, u.lastAccessedAt = :now WHERE u.shortCode = :code")
    fun incrementClickCount(code: String, now: Instant)

    @Modifying
    @Query("UPDATE UrlEntity u SET u.creationRequestCount = u.creationRequestCount + 1 WHERE u.urlHash = :hash")
    fun incrementCreationRequestCount(hash: String)
}
