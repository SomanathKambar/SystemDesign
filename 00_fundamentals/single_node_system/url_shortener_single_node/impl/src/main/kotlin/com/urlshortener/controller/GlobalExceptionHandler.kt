package com.urlshortener.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(e: IllegalArgumentException): ResponseEntity<Map<String, String>> {
        return ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Invalid request")))
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneralError(e: Exception): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(500).body(mapOf("error" to "An unexpected error occurred"))
    }

    @ExceptionHandler(Throwable::class)
    fun handleCriticalError(e: Throwable): ResponseEntity<Map<String, String>> {
        // Log the critical error (Senior logic: don't expose internal stack traces for critical errors)
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(mapOf("error" to "System is under extreme pressure and temporarily unavailable"))
    }
}
