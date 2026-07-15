package com.example.finanzas_independientes_app.core.network

import com.example.finanzas_independientes_app.core.session.SessionManager
import com.example.finanzas_independientes_app.data.remote.dto.RefreshRequest
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * Handles transparent token refresh on 401. OkHttp calls [authenticate] when a
 * request comes back 401; we exchange the refresh token, persist the rotated
 * pair, and retry the original request with the new access token.
 *
 * Concurrency: several requests can fail 401 at the same time. The
 * synchronized block plus the "did the token already change?" check ensures we
 * refresh once and the rest reuse the freshly stored token.
 */
class TokenAuthenticator(
    private val session: SessionManager,
    private val refreshApi: RefreshApi,
    private val onSessionExpired: () -> Unit
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Give up if we already retried this request once.
        if (responseCount(response) >= MAX_RETRIES) return null

        val tokenAtFailure = response.request.header("Authorization")?.removePrefix("Bearer ")

        synchronized(this) {
            val currentToken = session.accessToken

            // Another thread already refreshed while we waited on the lock.
            if (currentToken != null && currentToken != tokenAtFailure) {
                return retryWith(response, currentToken)
            }

            val refresh = session.refreshToken ?: run {
                expireSession()
                return null
            }

            val newAuth = runCatching { refreshApi.refresh(RefreshRequest(refresh)).execute() }
                .getOrNull()
            val body = newAuth?.body()?.data

            return if (newAuth != null && newAuth.isSuccessful && body != null) {
                session.saveSession(body)
                retryWith(response, body.token)
            } else {
                // Refresh rejected (REFRESH_TOKEN_INVALIDO or any failure): session is dead.
                expireSession()
                null
            }
        }
    }

    private fun retryWith(response: Response, token: String): Request =
        response.request.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

    private fun expireSession() {
        session.clear()
        onSessionExpired()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private companion object {
        const val MAX_RETRIES = 2
    }
}
