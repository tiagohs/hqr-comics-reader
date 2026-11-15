package com.tiagohs.hqr.data.net

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.Darwin
import platform.Foundation.NSURLRequestReloadIgnoringLocalCacheData

/** URLSession without its own cache: HQR keeps images in [ImageCache] and the rest is fetched fresh. */
actual fun httpClient(block: HttpClientConfig<*>.() -> Unit): HttpClient =
    HttpClient(Darwin) {
        engine {
            configureSession {
                setURLCache(null)
                setRequestCachePolicy(NSURLRequestReloadIgnoringLocalCacheData)
            }
        }
        block()
    }
