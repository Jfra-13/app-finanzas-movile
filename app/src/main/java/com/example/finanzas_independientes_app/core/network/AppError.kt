package com.example.finanzas_independientes_app.core.network

/**
 * Domain-level error. The UI never sees a Retrofit `Response` or a raw
 * exception — only one of these branches.
 */
sealed class AppError {

    /** Backend answered with a non-2xx envelope. Branch on [code]. */
    data class Api(
        val code: ApiCode,
        val rawCode: String,
        val httpStatus: Int,
        val message: String?,
        val fieldErrors: List<FieldError> = emptyList()
    ) : AppError()

    /** No connectivity, timeout, or any I/O failure reaching the server. */
    data object Network : AppError()

    /** Anything unexpected (parse failure, null body, programming error). */
    data class Unexpected(val cause: Throwable) : AppError()
}

/** Field-scoped validation error, mapped from the envelope `details[]`. */
data class FieldError(
    val field: String,
    val message: String
)
