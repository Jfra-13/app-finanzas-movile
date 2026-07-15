package com.example.finanzas_independientes_app.core.network

import com.example.finanzas_independientes_app.core.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Injects `Authorization: Bearer <token>` on protected routes. Public auth
 * routes are skipped so login/registro/refresh never carry a stale token.
 */
class AuthInterceptor(private val session: SessionManager) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (isPublic(request.url.encodedPath)) {
            return chain.proceed(request)
        }
        val token = session.accessToken ?: return chain.proceed(request)
        val authorized = request.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        return chain.proceed(authorized)
    }

    private fun isPublic(path: String): Boolean = PUBLIC_PATHS.any { path.endsWith(it) }

    private companion object {
        val PUBLIC_PATHS = listOf(
            "/usuarios/registro",
            "/usuarios/login",
            "/usuarios/refresh",
            "/usuarios/forgot-password",
            "/usuarios/verify-otp",
            "/usuarios/reset-password"
        )
    }
}
