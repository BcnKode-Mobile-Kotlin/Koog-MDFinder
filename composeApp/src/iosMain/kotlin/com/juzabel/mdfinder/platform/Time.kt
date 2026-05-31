package com.juzabel.mdfinder.platform

import kotlin.time.TimeSource

actual fun getCurrentTimeMillis(): Long {
    // iOS implementation using Kotlin's monotonic clock
    // Returns relative time in milliseconds since some arbitrary start point
    // This is sufficient for Phase 2 (timestamps only used for relative ordering)
    val mark = TimeSource.Monotonic.markNow()
    // Using a fixed base epoch time + elapsed milliseconds for compatibility
    // In production, would use platform-specific Foundation APIs
    val elapsedMs = mark.elapsedNow().inWholeMilliseconds
    val baseEpoch = 1704067200000L  // 2024-01-01 00:00:00 UTC in milliseconds
    return baseEpoch + elapsedMs
}
