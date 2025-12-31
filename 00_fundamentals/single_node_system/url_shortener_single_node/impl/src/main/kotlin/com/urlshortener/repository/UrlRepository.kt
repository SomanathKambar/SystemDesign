package com.urlshortener.repository

import com.urlshortener.model.UrlEntity
import org.springframework.data.jpa.repository.JpaRepository

interface UrlRepository : JpaRepository<UrlEntity, Long> {
    fun findByShortCode(code: String): UrlEntity?
}
