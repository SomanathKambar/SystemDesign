package com.urlshortener.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "urls")
data class UrlEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, length = 8, nullable = false)
    val shortCode: String,

    @Column(columnDefinition = "TEXT", nullable = false)
    val longUrl: String,

    val createdAt: Instant = Instant.now()
)