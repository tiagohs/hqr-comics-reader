package com.tiagohs.hqr.presentation.common

import com.tiagohs.hqr.domain.source.SourceException

/** Every screen with remote data has these states, each with a way out (RF-86). */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>

    data class Empty(
        val reason: EmptyReason = EmptyReason.NOTHING,
    ) : UiState<Nothing>

    data class Error(
        val kind: ErrorKind,
        val detail: String? = null,
    ) : UiState<Nothing>

    data class Content<T>(
        val value: T,
    ) : UiState<T>
}

enum class EmptyReason { NOTHING, NO_RESULTS, NO_FOLDER }

/** What the screen says and which action fixes it (design.md: "Erros"). */
enum class ErrorKind { OFFLINE, NETWORK, SERVER, PARSE, NOT_FOUND, AUTH, CHALLENGE, UNAVAILABLE, CERTIFICATE, BRIDGE, UNKNOWN }

fun Throwable.toErrorKind(): ErrorKind =
    when (this) {
        is SourceException ->
            when (kind) {
                SourceException.Kind.NETWORK -> ErrorKind.NETWORK
                SourceException.Kind.SERVER -> ErrorKind.SERVER
                SourceException.Kind.PARSE -> ErrorKind.PARSE
                SourceException.Kind.NOT_FOUND -> ErrorKind.NOT_FOUND
                SourceException.Kind.AUTH -> ErrorKind.AUTH
                SourceException.Kind.CHALLENGE -> ErrorKind.CHALLENGE
                SourceException.Kind.UNAVAILABLE -> ErrorKind.UNAVAILABLE
                SourceException.Kind.CERTIFICATE -> ErrorKind.CERTIFICATE
                SourceException.Kind.BRIDGE -> ErrorKind.BRIDGE
            }
        else -> ErrorKind.UNKNOWN
    }

/** Rethrows coroutine cancellation; anything else becomes a result. */
suspend inline fun <T> attempt(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (
        @Suppress("TooGenericExceptionCaught") e: Exception,
    ) {
        Result.failure(e)
    }
