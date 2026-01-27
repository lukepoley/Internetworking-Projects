package com.networkdetect

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.networkdetect.model.NetworkDevice
import com.networkdetect.model.SignalEstimate
import com.networkdetect.model.SpeedTestState
import com.networkdetect.model.WifiNetwork
import com.networkdetect.scanner.DeviceScanner
import com.networkdetect.scanner.WifiScanner
import com.networkdetect.speed.SpeedTester
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Main ViewModel that coordinates all network scanning services
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val wifiScanner = WifiScanner(application)
    private val deviceScanner = DeviceScanner(application)
    private val speedTester = SpeedTester()

    // WiFi state
    val currentNetwork: StateFlow<WifiNetwork?> = wifiScanner.currentNetwork
    val nearbyNetworks: StateFlow<List<WifiNetwork>> = wifiScanner.nearbyNetworks
    
    private val _signalEstimate = MutableStateFlow<SignalEstimate?>(null)
    val signalEstimate: StateFlow<SignalEstimate?> = _signalEstimate

    // Device state
    val devices: StateFlow<List<NetworkDevice>> = deviceScanner.devices
    val deviceScanProgress: StateFlow<Int> = deviceScanner.scanProgress
    val isDeviceScanning: StateFlow<Boolean> = deviceScanner.isScanning

    // Speed state
    val speedTestState: StateFlow<SpeedTestState> = speedTester.state

    // General state
    private val _isWifiEnabled = MutableStateFlow(true)
    val isWifiEnabled: StateFlow<Boolean> = _isWifiEnabled

    private var wifiMonitorJob: Job? = null

    init {
        startWifiMonitoring()
    }

    /**
     * Start continuous WiFi monitoring
     */
    fun startWifiMonitoring() {
        wifiMonitorJob?.cancel()
        wifiMonitorJob = viewModelScope.launch {
            while (isActive) {
                _isWifiEnabled.value = wifiScanner.isWifiEnabled()
                if (_isWifiEnabled.value) {
                    wifiScanner.getCurrentConnectionInfo()
                    wifiScanner.scanNearbyNetworks()
                    _signalEstimate.value = wifiScanner.estimateSignalDirection()
                }
                delay(2000) // Update every 2 seconds
            }
        }
    }

    /**
     * Stop WiFi monitoring
     */
    fun stopWifiMonitoring() {
        wifiMonitorJob?.cancel()
        wifiMonitorJob = null
    }

    /**
     * Clear signal history for fresh direction detection
     */
    fun clearSignalHistory() {
        wifiScanner.clearHistory()
        _signalEstimate.value = null
    }

    /**
     * Scan for devices on the network
     */
    fun scanDevices() {
        viewModelScope.launch {
            deviceScanner.scanNetwork()
        }
    }

    /**
     * Start NSD discovery
     */
    fun startNsdDiscovery() {
        deviceScanner.startNsdDiscovery()
    }

    /**
     * Stop NSD discovery
     */
    fun stopNsdDiscovery() {
        deviceScanner.stopNsdDiscovery()
    }

    /**
     * Run a speed test
     */
    fun runSpeedTest() {
        viewModelScope.launch {
            speedTester.runSpeedTest()
        }
    }

    /**
     * Reset speed test state
     */
    fun resetSpeedTest() {
        speedTester.reset()
    }

    /**
     * Get this device's IP
     */
    fun getOwnIp(): String? = deviceScanner.getOwnIp()

    override fun onCleared() {
        super.onCleared()
        stopWifiMonitoring()
        stopNsdDiscovery()
    }
}
