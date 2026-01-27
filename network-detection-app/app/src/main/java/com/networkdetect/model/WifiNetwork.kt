package com.networkdetect.model

/**
 * Represents the quality of a WiFi signal
 */
enum class SignalQuality {
    EXCELLENT,  // >= -50 dBm
    GOOD,       // -50 to -60 dBm
    FAIR,       // -60 to -70 dBm
    WEAK,       // -70 to -80 dBm
    POOR        // < -80 dBm
}

/**
 * Represents a WiFi network with signal information
 */
data class WifiNetwork(
    val ssid: String,
    val bssid: String,
    val signalStrength: Int,         // RSSI in dBm
    val signalQuality: SignalQuality,
    val signalPercentage: Int,       // 0-100%
    val frequency: Int,              // MHz
    val channel: Int,
    val is5GHz: Boolean,
    val capabilities: String,
    val isConnected: Boolean = false
) {
    companion object {
        /**
         * Convert RSSI (dBm) to signal quality
         */
        fun rssiToQuality(rssi: Int): SignalQuality = when {
            rssi >= -50 -> SignalQuality.EXCELLENT
            rssi >= -60 -> SignalQuality.GOOD
            rssi >= -70 -> SignalQuality.FAIR
            rssi >= -80 -> SignalQuality.WEAK
            else -> SignalQuality.POOR
        }

        /**
         * Convert RSSI (dBm) to percentage (0-100)
         */
        fun rssiToPercentage(rssi: Int): Int {
            return when {
                rssi >= -50 -> 100
                rssi <= -100 -> 0
                else -> 2 * (rssi + 100)
            }.coerceIn(0, 100)
        }

        /**
         * Convert frequency to channel number
         */
        fun frequencyToChannel(freq: Int): Int = when {
            freq in 2412..2484 -> (freq - 2412) / 5 + 1
            freq in 5170..5825 -> (freq - 5170) / 5 + 34
            else -> 0
        }
    }
}

/**
 * Signal direction estimation result
 */
data class SignalEstimate(
    val trend: SignalTrend,
    val message: String,
    val averageRssi: Int
)

enum class SignalTrend {
    IMPROVING,
    STABLE,
    WEAKENING
}
