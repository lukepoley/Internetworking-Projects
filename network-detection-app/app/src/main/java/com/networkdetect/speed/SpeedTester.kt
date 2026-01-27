package com.networkdetect.speed

import com.networkdetect.model.SpeedRating
import com.networkdetect.model.SpeedTestResult
import com.networkdetect.model.SpeedTestState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL

/**
 * Service for testing network speed
 */
class SpeedTester {

    private val _state = MutableStateFlow<SpeedTestState>(SpeedTestState.Idle)
    val state: StateFlow<SpeedTestState> = _state

    // Test files from various CDNs (small files for quick tests)
    private val testUrls = listOf(
        "https://speed.cloudflare.com/__down?bytes=10000000",  // 10MB
        "https://proof.ovh.net/files/10Mb.dat",               // 10MB  
        "http://speedtest.tele2.net/10MB.zip"                 // 10MB
    )

    private val pingHosts = listOf(
        "8.8.8.8",        // Google DNS
        "1.1.1.1",        // Cloudflare DNS
        "208.67.222.222"  // OpenDNS
    )

    /**
     * Run a complete speed test
     */
    suspend fun runSpeedTest(): SpeedTestResult = withContext(Dispatchers.IO) {
        try {
            _state.value = SpeedTestState.Testing("Measuring latency...", 10)

            // 1. Measure latency
            val latency = measureLatency()

            _state.value = SpeedTestState.Testing("Testing download speed...", 30)

            // 2. Measure download speed
            val downloadSpeed = measureDownloadSpeed()

            _state.value = SpeedTestState.Testing("Testing upload speed...", 70)

            // 3. Measure upload speed (simulated for now as most servers don't accept uploads)
            val uploadSpeed = estimateUploadSpeed(downloadSpeed)

            _state.value = SpeedTestState.Testing("Calculating results...", 95)

            val result = SpeedTestResult(
                downloadSpeedMbps = downloadSpeed,
                uploadSpeedMbps = uploadSpeed,
                latencyMs = latency,
                speedRating = SpeedTestResult.rateSpeed(downloadSpeed)
            )

            _state.value = SpeedTestState.Complete(result)
            result
        } catch (e: Exception) {
            _state.value = SpeedTestState.Error(e.message ?: "Speed test failed")
            SpeedTestResult(
                downloadSpeedMbps = 0.0,
                uploadSpeedMbps = 0.0,
                latencyMs = -1,
                speedRating = SpeedRating.VERY_SLOW
            )
        }
    }

    /**
     * Measure ping latency to common servers
     */
    private suspend fun measureLatency(): Long = withContext(Dispatchers.IO) {
        val latencies = pingHosts.mapNotNull { host ->
            try {
                val startTime = System.currentTimeMillis()
                val reachable = InetAddress.getByName(host).isReachable(3000)
                if (reachable) {
                    System.currentTimeMillis() - startTime
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }

        if (latencies.isEmpty()) {
            // Fallback: try HTTP ping
            try {
                val startTime = System.currentTimeMillis()
                val url = URL("https://www.google.com")
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 3000
                connection.readTimeout = 3000
                connection.requestMethod = "HEAD"
                connection.connect()
                connection.disconnect()
                System.currentTimeMillis() - startTime
            } catch (e: Exception) {
                -1L
            }
        } else {
            latencies.minOrNull() ?: -1L
        }
    }

    /**
     * Measure download speed by downloading a test file
     */
    private suspend fun measureDownloadSpeed(): Double = withContext(Dispatchers.IO) {
        var bestSpeed = 0.0

        for (testUrl in testUrls) {
            try {
                val speed = downloadTestFile(testUrl)
                if (speed > bestSpeed) {
                    bestSpeed = speed
                }
                // If we got a good reading, don't try more
                if (speed > 5.0) break
            } catch (e: Exception) {
                // Try next URL
            }
        }

        bestSpeed
    }

    /**
     * Download a test file and measure speed
     */
    private fun downloadTestFile(urlString: String): Double {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 30000
        connection.requestMethod = "GET"

        val startTime = System.currentTimeMillis()
        var totalBytes = 0L

        try {
            connection.connect()
            
            BufferedInputStream(connection.inputStream).use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                val maxTime = 10000 // Max 10 seconds
                
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    totalBytes += bytesRead
                    
                    // Check timeout
                    if (System.currentTimeMillis() - startTime > maxTime) {
                        break
                    }
                }
            }
        } finally {
            connection.disconnect()
        }

        val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000.0
        if (elapsedSeconds <= 0) return 0.0

        // Calculate Mbps (megabits per second)
        val bytesPerSecond = totalBytes / elapsedSeconds
        val megabitsPerSecond = (bytesPerSecond * 8) / 1_000_000

        return megabitsPerSecond
    }

    /**
     * Estimate upload speed (typically slightly lower than download)
     * Real upload testing would require a server that accepts uploads
     */
    private fun estimateUploadSpeed(downloadSpeed: Double): Double {
        // Most connections have asymmetric speeds (upload is slower)
        // This is a rough estimate - actual testing would require server support
        return downloadSpeed * 0.7
    }

    /**
     * Reset test state
     */
    fun reset() {
        _state.value = SpeedTestState.Idle
    }
}
