package com.example.finanzas_independientes_app.core.network

/**
 * Uniform envelope returned by every backend response (success and error).
 * The client decides flow by [code], never by [message].
 */
data class ApiResponse<T>(
    val timestamp: String? = null,
    val status: Int = 0,
    val code: String = "",
    val message: String? = null,
    val data: T? = null,
    val path: String? = null,
    val details: List<ErrorDetail>? = null
)

/** Single field validation error, present only on VALIDATION_ERROR responses. */
data class ErrorDetail(
    val field: String? = null,
    val rejectedValue: String? = null,
    val message: String? = null
)
