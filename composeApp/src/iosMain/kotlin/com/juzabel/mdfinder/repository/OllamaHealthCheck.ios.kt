package com.juzabel.mdfinder.repository

/**
 * iOS-specific implementation of HTTP health check.
 * In Phase 2, this returns a stub indicating health check not available on iOS (desktop-only phase).
 * Phase 3+ will implement proper URLSession-based health checks if iOS support is added.
 */
actual suspend fun performHealthCheck(endpoint: String): Boolean {
    // Phase 2: iOS support not included (desktop/Android focus)
    // Return false to indicate Ollama unreachable (safe default)
    // When Phase 3 adds iOS, implement using URLSession
    return false
}
