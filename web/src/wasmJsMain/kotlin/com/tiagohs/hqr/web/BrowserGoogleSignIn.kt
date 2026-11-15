package com.tiagohs.hqr.web

import com.tiagohs.hqr.domain.repository.GoogleIdTokens
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.encodeURLParameter
import kotlinx.browser.sessionStorage
import kotlinx.browser.window
import kotlinx.coroutines.awaitCancellation
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.random.Random

/**
 * RF-119 in the browser: Google's own page (OpenID Connect, ID token in the address), no Google script on the site.
 * The web client id comes from the Worker; the return address is the app itself, which finishes in Conta e
 * sincronização (ADR-028).
 */
class BrowserGoogleSignIn(
    private val http: HttpClient,
    private val origin: String,
) : GoogleIdTokens {
    private var returned: String? = takeTokenFromAddress()

    override suspend fun request(): String? {
        val clientId =
            runCatching {
                Json
                    .parseToJsonElement(http.get("$origin/api/v1/config").bodyAsText())
                    .jsonObject["googleClientId"]
                    ?.jsonPrimitive
                    ?.content
            }.getOrNull() ?: return null
        val state = Random.nextBytes(STATE_BYTES).joinToString("") { (it.toInt() and BYTE_MASK).toString(HEX).padStart(2, '0') }
        sessionStorage.setItem(STATE_KEY, state)
        val query =
            listOf(
                "client_id" to clientId,
                "redirect_uri" to "$origin/",
                "response_type" to "id_token",
                "scope" to "openid email",
                "nonce" to state,
                "state" to state,
                "prompt" to "select_account",
            ).joinToString("&") { (k, v) -> "$k=${v.encodeURLParameter()}" }
        window.location.href = "https://accounts.google.com/o/oauth2/v2/auth?$query"
        // The page leaves for Google; nothing comes back here.
        awaitCancellation()
    }

    override fun pending(): String? = returned.also { returned = null }

    private companion object {
        const val STATE_KEY = "hqr.signin.state"
        const val STATE_BYTES = 16
        const val BYTE_MASK = 0xFF
        const val HEX = 16

        /** `#id_token=…&state=…` from Google, checked against the state saved before leaving; the address becomes the account screen. */
        fun takeTokenFromAddress(): String? {
            val hash = window.location.hash.removePrefix("#")
            if (!hash.contains("id_token=") && !hash.contains("error=")) return null
            val params =
                hash.split('&').mapNotNull { part ->
                    part.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0] to decode(it[1]) }
                }.toMap()
            val expected = sessionStorage.getItem(STATE_KEY)
            sessionStorage.removeItem(STATE_KEY)
            window.history.replaceState(null, "", "${window.location.pathname}#account")
            return params["id_token"]?.takeIf { expected != null && params["state"] == expected }
        }
    }
}

@Suppress("UnusedParameter") // Read by the JavaScript below.
private fun decode(value: String): String = js("decodeURIComponent(value)")
