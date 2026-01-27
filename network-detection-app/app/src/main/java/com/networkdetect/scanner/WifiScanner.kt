package com.networkdetect.scanner

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.networkdetect.model.SignalEstimate
import com.networkdetect.model.SignalTrend
import com.networkdetect.model.WifiNetwork
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Service for scanning WiFi networks and monitoring signal strength
 */
class WifiScanner(private val context: Context) {

    private val wifiManager: WifiManager = 
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val _currentNetwork = MutableStateFlow<WifiNetwork?>(null)
    val currentNetwork: StateFlow<WifiNetwork?> = _currentNetwork

    private val _nearbyNetworks = MutableStateFlow<List<WifiNetwork>>(emptyList())
    val nearbyNetworks: StateFlow<List<WifiNetwork>> = _nearbyNetworks

    // History of RSSI values for signal direction estimation
    private val rssiHistory = mutableListOf<Int>()
    private val maxHistorySize = 10

    /**
     * Get the currently connected WiFi network info
     */
    fun getCurrentConnectionInfo(): WifiNetwork? {
        if (!hasPermissions()) return null
        
        val connectionInfo = wifiManager.connectionInfo ?: return null
        
        // Check if actually connected
        if (connectionInfo.networkId == -1) return null
        
        val ssid = connectionInfo.ssid?.removeSurrounding("\"") ?: "<Unknown>"
        val rssi = connectionInfo.rssi
        val frequency = connectionInfo.frequency
        
        // Update RSSI history for direction estimation
        updateRssiHistory(rssi)
        
        val network = WifiNetwork(
            ssid = ssid,
            bssid = connectionInfo.bssid ?: "",
            signalStrength = rssi,
            signalQuality = WifiNetwork.rssiToQuality(rssi),
            signalPercentage = WifiNetwork.rssiToPercentage(rssi),
            frequency = frequency,
            channel = WifiNetwork.frequencyToChannel(frequency),
            is5GHz = frequency > 5000,
            capabilities = "",
            isConnected = true
        )
        
        _currentNetwork.value = network
        return network
    }

    /**
     * Scan for nearby WiFi networks
     */
    @Suppress("DEPRECATION")
    fun scanNearbyNetworks(): List<WifiNetwork> {
        if (!hasPermissions()) return emptyList()

        // Trigger a new scan
        wifiManager.startScan()

        // Get scan results
        val scanResults = wifiManager.scanResults ?: return emptyList()
        val currentBssid = wifiManager.connectionInfo?.bssid

        val networks = scanResults.map { result ->
            val rssi = result.level
            val frequency = result.frequency
            
            WifiNetwork(
                ssid = result.SSID.ifEmpty { "<Hidden Network>" },
                bssid = result.BSSID,
                signalStrength = rssi,
                signalQuality = WifiNetwork.rssiToQuality(rssi),
                signalPercentage = WifiNetwork.rssiToPercentage(rssi),
                frequency = frequency,
                channel = WifiNetwork.frequencyToChannel(frequency),
                is5GHz = frequency > 5000,
                capabilities = result.capabilities,
                isConnected = result.BSSID == currentBssid
            )
        }.sortedByDescending { it.signalStrength }

        _nearbyNetworks.value = networks
        return networks
    }

    /**
     * Get scan results as a Flow that updates when scans complete
     */
    fun scanResultsFlow(): Flow<List<WifiNetwork>> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    intent?.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false) ?: false
                } else {
                    true
                }
                
                if (success) {
                    trySend(scanNearbyNetworks())
                }
            }
        }

        context.registerReceiver(
            receiver,
            IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        )

        // Initial scan
        wifiManager.startScan()

        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }

    /**
     * Estimate signal direction based on RSSI changes
     */
    fun estimateSignalDirection(): SignalEstimate {
        if (rssiHistory.size < 3) {
            return SignalEstimate(
                trend = SignalTrend.STABLE,
                message = "Move around to detect signal direction",
                averageRssi = rssiHistory.lastOrNull() ?: -100
            )
        }

        val recentValues = rssiHistory.takeLast(5)
        val olderValues = rssiHistory.dropLast(rssiHistory.size / 2).takeLast(5)

        val recentAvg = recentValues.average()
        val olderAvg = if (olderValues.isNotEmpty()) olderValues.average() else recentAvg
        val trend = recentAvg - olderAvg

        return when {
            trend > 3 -> SignalEstimate(
                trend = SignalTrend.IMPROVING,
                message = "📶 Signal getting stronger! Keep moving this direction",
                averageRssi = recentAvg.toInt()
            )
            trend < -3 -> SignalEstimate(
                trend = SignalTrend.WEAKENING,
                message = "📉 Signal weakening. Try a different direction",
                averageRssi = recentAvg.toInt()
            )
            else -> SignalEstimate(
                trend = SignalTrend.STABLE,
                message = "📡 Signal stable. You may be near the router or an obstacle",
                averageRssi = recentAvg.toInt()
            )
        }
    }

    /**
     * Clear signal history (call when starting fresh detection)
     */
    fun clearHistory() {
        rssiHistory.clear()
    }

    private fun updateRssiHistory(rssi: Int) {
        rssiHistory.add(rssi)
        if (rssiHistory.size > maxHistorySize) {
            rssiHistory.removeAt(0)
        }
    }

    private fun hasPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if WiFi is enabled
     */
    fun isWifiEnabled(): Boolean = wifiManager.isWifiEnabled
}
