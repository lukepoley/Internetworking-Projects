package com.networkdetect.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.networkdetect.MainActivity
import com.networkdetect.R
import com.networkdetect.databinding.FragmentDevicesBinding
import com.networkdetect.databinding.ItemDeviceBinding
import com.networkdetect.model.DeviceType
import com.networkdetect.model.NetworkDevice
import kotlinx.coroutines.launch

class DevicesFragment : Fragment() {

    private var _binding: FragmentDevicesBinding? = null
    private val binding get() = _binding!!

    private val viewModel by lazy { (requireActivity() as MainActivity).viewModel }
    private lateinit var adapter: DeviceAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDevicesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSwipeRefresh()
        
        binding.btnScan.setOnClickListener {
            viewModel.scanDevices()
        }

        observeData()
    }

    private fun setupRecyclerView() {
        adapter = DeviceAdapter()
        binding.recyclerDevices.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerDevices.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.scanDevices()
        }
        binding.swipeRefresh.setColorSchemeResources(
            R.color.primary,
            R.color.signal_excellent,
            R.color.signal_good
        )
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.devices.collect { devices ->
                        adapter.submitList(devices)
                        binding.tvDeviceCount.text = "${devices.size} devices found"
                        
                        binding.layoutEmpty.visibility = if (devices.isEmpty()) View.VISIBLE else View.GONE
                        binding.recyclerDevices.visibility = if (devices.isEmpty()) View.GONE else View.VISIBLE
                    }
                }

                launch {
                    viewModel.isDeviceScanning.collect { isScanning ->
                        binding.swipeRefresh.isRefreshing = isScanning
                        binding.btnScan.isEnabled = !isScanning
                        binding.btnScan.text = if (isScanning) "Scanning..." else "Scan Network"
                    }
                }

                launch {
                    viewModel.deviceScanProgress.collect { progress ->
                        binding.progressScan.progress = progress
                        binding.progressScan.visibility = if (progress in 1..99) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Adapter for displaying network devices
     */
    inner class DeviceAdapter : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

        private var devices: List<NetworkDevice> = emptyList()

        fun submitList(newDevices: List<NetworkDevice>) {
            devices = newDevices
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
            val binding = ItemDeviceBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return DeviceViewHolder(binding)
        }

        override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
            holder.bind(devices[position])
        }

        override fun getItemCount(): Int = devices.size

        inner class DeviceViewHolder(
            private val binding: ItemDeviceBinding
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(device: NetworkDevice) {
                binding.tvDeviceName.text = device.getDisplayName()
                binding.tvDeviceIp.text = device.ipAddress
                binding.tvDeviceType.text = device.deviceType.displayName
                
                // Response time indicator
                val responseText = if (device.responseTimeMs > 0) {
                    "${device.responseTimeMs}ms"
                } else {
                    "Active"
                }
                binding.tvResponseTime.text = responseText

                // Highlight slow devices
                if (device.isPotentiallySlowingNetwork()) {
                    binding.tvResponseTime.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.signal_weak)
                    )
                    binding.cardDevice.strokeWidth = 2
                    binding.cardDevice.strokeColor = 
                        ContextCompat.getColor(requireContext(), R.color.signal_weak)
                } else {
                    binding.tvResponseTime.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.text_secondary)
                    )
                    binding.cardDevice.strokeWidth = 0
                }

                // Set icon based on device type
                val iconRes = when (device.deviceType) {
                    DeviceType.ROUTER -> R.drawable.ic_router
                    DeviceType.COMPUTER -> R.drawable.ic_computer
                    DeviceType.PHONE -> R.drawable.ic_phone
                    DeviceType.SMART_TV -> R.drawable.ic_tv
                    DeviceType.STREAMING_DEVICE -> R.drawable.ic_cast
                    DeviceType.GAMING_CONSOLE -> R.drawable.ic_games
                    DeviceType.IOT_DEVICE -> R.drawable.ic_devices
                    DeviceType.PRINTER -> R.drawable.ic_print
                    DeviceType.CAMERA -> R.drawable.ic_camera
                    DeviceType.SPEAKER -> R.drawable.ic_speaker
                    DeviceType.UNKNOWN -> R.drawable.ic_device_unknown
                }
                binding.ivDeviceIcon.setImageResource(iconRes)
            }
        }
    }
}
