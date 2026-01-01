package com.urlshortener.component

import org.springframework.stereotype.Component

interface ShortCodeGenerator {
    fun generate(): String
}

@Component
class RandomShortCodeGenerator : ShortCodeGenerator {
    private val charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    private val length = 8

    override fun generate(): String {
        return (1..length)
            .map { charset.random() }
            .joinToString("")
    }
}
