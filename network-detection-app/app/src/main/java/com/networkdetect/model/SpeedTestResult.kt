package com.networkdetect.model

/**
 * Result of a network speed test
 */
data class SpeedTestResult(
    val downloadSpeedMbps: Double,
    val uploadSpeedMbps: Double,
    val latencyMs: Long,
    val speedRating: SpeedRating,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * Rate the speed based on download Mbps
         */
        fun rateSpeed(downloadMbps: Double): SpeedRating = when {
            downloadMbps >= 50.0 -> SpeedRating.VERY_FAST
            downloadMbps >= 25.0 -> SpeedRating.FAST
            downloadMbps >= 10.0 -> SpeedRating.MODERATE
            downloadMbps >= 5.0 -> SpeedRating.SLOW
            else -> SpeedRating.VERY_SLOW
        }
    }
}

/**
 * Speed rating categories
 */
enum class SpeedRating(val displayName: String, val color: String) {
    VERY_FAST("Very Fast", "#4CAF50"),      // Green
    FAST("Fast", "#8BC34A"),                 // Light Green
    MODERATE("Moderate", "#FFC107"),         // Amber
    SLOW("Slow", "#FF9800"),                 // Orange
    VERY_SLOW("Very Slow", "#F44336");       // Red

    fun getDescription(): String = when (this) {
        VERY_FAST -> "Excellent! Great for 4K streaming and large downloads"
        FAST -> "Good for HD streaming and video calls"
        MODERATE -> "Suitable for basic browsing and SD streaming"
        SLOW -> "May experience buffering on videos"
        VERY_SLOW -> "Network may be congested or signal weak"
    }
}

/**
 * State of speed test progress
 */
sealed class SpeedTestState {
    object Idle : SpeedTestState()
    data class Testing(val phase: String, val progress: Int) : SpeedTestState()
    data class Complete(val result: SpeedTestResult) : SpeedTestState()
    data class Error(val message: String) : SpeedTestState()
}
