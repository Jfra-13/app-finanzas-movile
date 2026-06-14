package com.example.finanzas_independientes_app.core.network

import com.google.gson.Gson
import retrofit2.Response
import java.io.IOException

/**
 * Wraps a Retrofit call that carries data and turns it into an [ApiResult].
 * Use this for endpoints whose success envelope has a non-null `data`.
 */
suspend fun <T> safeApiCall(
    gson: Gson,
    call: suspend () -> Response<ApiResponse<T>>
): ApiResult<T> = execute(gson, call) { body ->
    val data = body.data
    if (data != null) {
        ApiResult.Success(data, ApiCode.from(body.code))
    } else {
        ApiResult.Error(AppError.Unexpected(IllegalStateException("Respuesta exitosa sin data")))
    }
}

/**
 * Wraps a Retrofit call with no meaningful body (`data: null`). Returns
 * [Unit] on success while still surfacing the success [ApiCode].
 */
suspend fun safeUnitCall(
    gson: Gson,
    call: suspend () -> Response<ApiResponse<Unit>>
): ApiResult<Unit> = execute(gson, call) { body ->
    ApiResult.Success(Unit, ApiCode.from(body.code))
}

private suspend fun <T, R> execute(
    gson: Gson,
    call: suspend () -> Response<ApiResponse<T>>,
    onSuccessBody: (ApiResponse<T>) -> ApiResult<R>
): ApiResult<R> = try {
    val response = call()
    if (response.isSuccessful) {
        val body = response.body()
        if (body != null) onSuccessBody(body)
        else ApiResult.Error(AppError.Unexpected(IllegalStateException("Cuerpo de respuesta vacío")))
    } else {
        ApiResult.Error(parseError(gson, response))
    }
} catch (io: IOException) {
    ApiResult.Error(AppError.Network)
} catch (e: Exception) {
    ApiResult.Error(AppError.Unexpected(e))
}

/** Non-generic view of the envelope, used only to decode error bodies. */
private data class ErrorEnvelope(
    val code: String? = null,
    val message: String? = null,
    val details: List<ErrorDetail>? = null
)

/** Parses a non-2xx error body into a domain [AppError.Api]. */
private fun parseError(gson: Gson, response: Response<*>): AppError {
    val raw = response.errorBody()?.string()
    val envelope = raw?.let {
        runCatching { gson.fromJson(it, ErrorEnvelope::class.java) }.getOrNull()
    }
    val rawCode = envelope?.code ?: ""
    val fieldErrors = envelope?.details.orEmpty().mapNotNull { detail ->
        val field = detail.field ?: return@mapNotNull null
        FieldError(field, detail.message ?: "")
    }
    return AppError.Api(
        code = ApiCode.from(rawCode),
        rawCode = rawCode,
        httpStatus = response.code(),
        message = envelope?.message,
        fieldErrors = fieldErrors
    )
}
