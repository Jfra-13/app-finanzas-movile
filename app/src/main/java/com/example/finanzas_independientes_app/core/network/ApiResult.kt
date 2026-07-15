package com.example.finanzas_independientes_app.core.network

/**
 * Outcome of a network call expressed in domain terms. Every API call returns
 * one of these — the UI pattern-matches on [Success] / [Error] and never
 * touches Retrofit or exceptions directly.
 */
sealed interface ApiResult<out T> {

    data class Success<T>(val data: T, val code: ApiCode) : ApiResult<T>

    data class Error(val error: AppError) : ApiResult<Nothing>
}

inline fun <T> ApiResult<T>.onSuccess(action: (T) -> Unit): ApiResult<T> {
    if (this is ApiResult.Success) action(data)
    return this
}

inline fun <T> ApiResult<T>.onError(action: (AppError) -> Unit): ApiResult<T> {
    if (this is ApiResult.Error) action(error)
    return this
}
