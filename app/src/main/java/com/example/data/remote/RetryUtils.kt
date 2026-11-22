package com.example.data.remote

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/**
 * Retries [block] using exponential backoff when a transient [IOException] occurs
 * or [shouldRetry] returns true for the returned value.
 */
internal suspend fun <T> executeWithExponentialBackoff(
    maxAttempts: Int = 3,
    initialDelayMs: Long = 400L,
    maxDelayMs: Long = 2_000L,
    delayFn: suspend (Long) -> Unit = { delay(it) },
    shouldRetry: (T) -> Boolean = { false },
    block: suspend () -> T,
): T {
    require(maxAttempts >= 1) { "maxAttempts must be at least 1" }
    var currentDelay = initialDelayMs
    var lastError: IOException? = null

    for (attempt in 1..maxAttempts) {
        try {
            val result = block()
            if (!shouldRetry(result) || attempt == maxAttempts) {
                return result
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            lastError = e
            if (attempt == maxAttempts) {
                throw e
            }
        }

        delayFn(currentDelay)
        currentDelay = (currentDelay * 2).coerceAtMost(maxDelayMs)
    }

    throw lastError ?: IllegalStateException("Retry exhausted without result")
}
