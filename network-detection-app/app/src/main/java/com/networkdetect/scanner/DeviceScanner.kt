package com.networkdetect.scanner

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import com.networkdetect.model.DeviceType
import com.networkdetect.model.NetworkDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Service for discovering devices on the local network
 */
class DeviceScanner(private val context: Context) {

    private val wifiManager: WifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val nsdManager: NsdManager =
        context.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager

    private val _devices = MutableStateFlow<List<NetworkDevice>>(emptyList())
    val devices: StateFlow<List<NetworkDevice>> = _devices

    private val _scanProgress = MutableStateFlow(0)
    val scanProgress: StateFlow<Int> = _scanProgress

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val discoveredDevices = mutableMapOf<String, NetworkDevice>()
    private var nsdDiscoveryListener: NsdManager.DiscoveryListener? = null

    /**
     * Scan for devices on the local network using IP scanning
     */
    suspend fun scanNetwork(): List<NetworkDevice> = withContext(Dispatchers.IO) {
        _isScanning.value = true
        _scanProgress.value = 0
        discoveredDevices.clear()

        try {
            val subnet = getLocalSubnet() ?: return@withContext emptyList()
            
            // Scan IP range in batches for efficiency
            val batchSize = 25
            val totalHosts = 254
            
            coroutineScope {
                (1..totalHosts).chunked(batchSize).forEachIndexed { batchIndex, batch ->
                    batch.map { hostNum ->
                        async {
                            val ip = "$subnet.$hostNum"
                            scanHost(ip)
                        }
                    }.awaitAll()
                    
                    // Update progress
                    _scanProgress.value = ((batchIndex + 1) * batchSize * 100 / totalHosts).coerceAtMost(100)
                }
            }

            val result = discoveredDevices.values.toList()
                .sortedBy { it.ipAddress.split(".").last().toIntOrNull() ?: 0 }
            
            _devices.value = result
            result
        } finally {
            _isScanning.value = false
            _scanProgress.value = 100
        }
    }

    /**
     * Start Network Service Discovery to find advertised services
     */
    fun startNsdDiscovery() {
        stopNsdDiscovery()

        val serviceTypes = listOf(
            "_http._tcp.",      // Web servers
            "_https._tcp.",     // Secure web
            "_airplay._tcp.",   // Apple AirPlay
            "_googlecast._tcp.", // Chromecast
            "_spotify-connect._tcp.", // Spotify
            "_printer._tcp.",   // Printers
            "_ipp._tcp."        // Internet Printing Protocol
        )

        // Just discover the most common type to avoid overwhelming NSD
        nsdDiscoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {}

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                // Resolve the service to get more details
                nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}

                    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                        val ip = serviceInfo.host?.hostAddress ?: return
                        val device = NetworkDevice(
                            ipAddress = ip,
                            hostname = serviceInfo.host?.hostName,
                            serviceName = serviceInfo.serviceName,
                            deviceType = DeviceType.fromName(serviceInfo.serviceName),
                            isReachable = true,
                            responseTimeMs = 0
                        )
                        discoveredDevices[ip] = device
                        _devices.value = discoveredDevices.values.toList()
                    }
                })
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {}
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }

        try {
            nsdManager.discoverServices("_services._dns-sd._udp.", NsdManager.PROTOCOL_DNS_SD, nsdDiscoveryListener)
        } catch (e: Exception) {
            // NSD might fail on some devices
        }
    }

    /**
     * Stop NSD discovery
     */
    fun stopNsdDiscovery() {
        nsdDiscoveryListener?.let {
            try {
                nsdManager.stopServiceDiscovery(it)
            } catch (e: Exception) {
                // May already be stopped
            }
        }
        nsdDiscoveryListener = null
    }

    /**
     * Quick scan that only checks if a device responds
     */
    private suspend fun scanHost(ip: String) {
        try {
            val startTime = System.currentTimeMillis()
            
            // Try to reach the host
            val reachable = isHostReachable(ip)
            
            if (reachable) {
                val responseTime = System.currentTimeMillis() - startTime
                
                // Try to get hostname
                val hostname = withTimeoutOrNull(500) {
                    try {
                        InetAddress.getByName(ip).hostName
                    } catch (e: Exception) {
                        null
                    }
                }

                val device = NetworkDevice(
                    ipAddress = ip,
                    hostname = if (hostname != ip) hostname else null,
                    deviceType = identifyDeviceType(ip, hostname),
                    isReachable = true,
                    responseTimeMs = responseTime
                )

                synchronized(discoveredDevices) {
                    discoveredDevices[ip] = device
                }
            }
        } catch (e: Exception) {
            // Host not reachable
        }
    }

    /**
     * Check if a host is reachable
     */
    private suspend fun isHostReachable(ip: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Try TCP connection to common ports
            val ports = listOf(80, 443, 7, 22, 8080)
            
            for (port in ports) {
                try {
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(ip, port), 100)
                        return@withContext true
                    }
                } catch (e: Exception) {
                    // Try next port
                }
            }
            
            // Fall back to ping
            val address = InetAddress.getByName(ip)
            address.isReachable(200)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Try to identify the device type
     */
    private fun identifyDeviceType(ip: String, hostname: String?): DeviceType {
        // Check if it's the router (gateway)
        val gatewayIp = getGatewayIp()
        if (ip == gatewayIp) {
            return DeviceType.ROUTER
        }

        // Try to identify from hostname
        return DeviceType.fromName(hostname)
    }

    /**
     * Get the local subnet (e.g., "192.168.1")
     */
    @Suppress("DEPRECATION")
    private fun getLocalSubnet(): String? {
        val dhcpInfo = wifiManager.dhcpInfo ?: return null
        val ip = dhcpInfo.ipAddress
        
        // Convert int IP to string octets
        val b1 = ip and 0xff
        val b2 = (ip shr 8) and 0xff
        val b3 = (ip shr 16) and 0xff
        
        return "$b1.$b2.$b3"
    }

    /**
     * Get the gateway IP address
     */
    @Suppress("DEPRECATION")
    private fun getGatewayIp(): String? {
        val dhcpInfo = wifiManager.dhcpInfo ?: return null
        val gateway = dhcpInfo.gateway
        
        val b1 = gateway and 0xff
        val b2 = (gateway shr 8) and 0xff
        val b3 = (gateway shr 16) and 0xff
        val b4 = (gateway shr 24) and 0xff
        
        return "$b1.$b2.$b3.$b4"
    }

    /**
     * Get the device's own IP address
     */
    @Suppress("DEPRECATION")
    fun getOwnIp(): String? {
        val dhcpInfo = wifiManager.dhcpInfo ?: return null
        val ip = dhcpInfo.ipAddress
        
        val b1 = ip and 0xff
        val b2 = (ip shr 8) and 0xff
        val b3 = (ip shr 16) and 0xff
        val b4 = (ip shr 24) and 0xff
        
        return "$b1.$b2.$b3.$b4"
    }
}
