package com.tiagohs.hqr.data.net

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * At most [permits] requests in each [period], shared by every use of one source: reader, search, downloads and
 * updates (RF-04, ADR-005). Callers wait their turn instead of failing.
 */
class RateLimiter(
    private val permits: Int,
    private val period: Duration,
    private val clock: Clock = Clock.System,
    private val sleep: suspend (Duration) -> Unit = { delay(it) },
) {
    private val mutex = Mutex()
    private val stamps = ArrayDeque<Long>()

    suspend fun acquire() {
        mutex.withLock {
            while (true) {
                val now = clock.now().toEpochMilliseconds()
                while (stamps.isNotEmpty() && now - stamps.first() >= period.inWholeMilliseconds) stamps.removeFirst()
                if (stamps.size < permits) {
                    stamps.addLast(now)
                    return
                }
                sleep((period.inWholeMilliseconds - (now - stamps.first())).coerceAtLeast(1).milliseconds)
            }
        }
    }
}
