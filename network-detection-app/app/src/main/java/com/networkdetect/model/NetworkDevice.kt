package com.networkdetect.model

/**
 * Represents a device found on the network
 */
data class NetworkDevice(
    val ipAddress: String,
    val hostname: String? = null,
    val macAddress: String? = null,
    val deviceType: DeviceType = DeviceType.UNKNOWN,
    val isReachable: Boolean = true,
    val responseTimeMs: Long = 0,
    val serviceName: String? = null  // For NSD discovered devices
) {
    /**
     * Returns a display name for the device
     */
    fun getDisplayName(): String {
        return hostname ?: serviceName ?: ipAddress
    }

    /**
     * Determine if this device might be causing network slowdown
     */
    fun isPotentiallySlowingNetwork(): Boolean {
        // Devices with high response time might be consuming bandwidth
        return responseTimeMs > 100 || deviceType == DeviceType.STREAMING_DEVICE
    }
}

/**
 * Type of network device
 */
enum class DeviceType(val displayName: String, val iconRes: String) {
    ROUTER("Router", "router"),
    COMPUTER("Computer", "computer"),
    PHONE("Phone/Tablet", "phone"),
    SMART_TV("Smart TV", "tv"),
    STREAMING_DEVICE("Streaming Device", "cast"),
    GAMING_CONSOLE("Gaming Console", "games"),
    IOT_DEVICE("IoT Device", "devices"),
    PRINTER("Printer", "print"),
    CAMERA("Camera", "camera"),
    SPEAKER("Smart Speaker", "speaker"),
    UNKNOWN("Unknown Device", "device_unknown");

    companion object {
        /**
         * Try to identify device type from hostname or service name
         */
        fun fromName(name: String?): DeviceType {
            if (name == null) return UNKNOWN
            val lower = name.lowercase()
            return when {
                lower.contains("router") || lower.contains("gateway") -> ROUTER
                lower.contains("iphone") || lower.contains("android") || 
                    lower.contains("pixel") || lower.contains("galaxy") -> PHONE
                lower.contains("macbook") || lower.contains("laptop") || 
                    lower.contains("desktop") || lower.contains("pc") -> COMPUTER
                lower.contains("tv") || lower.contains("roku") || 
                    lower.contains("firetv") -> SMART_TV
                lower.contains("chromecast") || lower.contains("appletv") -> STREAMING_DEVICE
                lower.contains("playstation") || lower.contains("xbox") || 
                    lower.contains("nintendo") -> GAMING_CONSOLE
                lower.contains("printer") || lower.contains("canon") || 
                    lower.contains("epson") || lower.contains("hp") -> PRINTER
                lower.contains("camera") || lower.contains("ring") || 
                    lower.contains("nest") -> CAMERA
                lower.contains("echo") || lower.contains("homepod") || 
                    lower.contains("sonos") -> SPEAKER
                lower.contains("esp") || lower.contains("arduino") || 
                    lower.contains("raspberry") -> IOT_DEVICE
                else -> UNKNOWN
            }
        }
    }
}
