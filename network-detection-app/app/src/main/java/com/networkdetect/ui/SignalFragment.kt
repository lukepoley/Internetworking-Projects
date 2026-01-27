package com.networkdetect.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.networkdetect.MainActivity
import com.networkdetect.R
import com.networkdetect.databinding.FragmentSignalBinding
import com.networkdetect.model.SignalQuality
import com.networkdetect.model.SignalTrend
import kotlinx.coroutines.launch

class SignalFragment : Fragment() {

    private var _binding: FragmentSignalBinding? = null
    private val binding get() = _binding!!

    private val viewModel by lazy { (requireActivity() as MainActivity).viewModel }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnResetDirection.setOnClickListener {
            viewModel.clearSignalHistory()
        }

        observeData()
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.currentNetwork.collect { network ->
                        if (network != null) {
                            binding.tvNetworkName.text = network.ssid
                            binding.tvSignalStrength.text = "${network.signalPercentage}%"
                            binding.tvSignalDbm.text = "${network.signalStrength} dBm"
                            binding.tvFrequency.text = if (network.is5GHz) "5 GHz" else "2.4 GHz"
                            binding.tvChannel.text = "Channel ${network.channel}"

                            // Update signal bars
                            updateSignalBars(network.signalQuality)
                            
                            // Update progress
                            binding.progressSignal.progress = network.signalPercentage
                        } else {
                            binding.tvNetworkName.text = "Not connected"
                            binding.tvSignalStrength.text = "--"
                            binding.tvSignalDbm.text = "-- dBm"
                            updateSignalBars(SignalQuality.POOR)
                        }
                    }
                }

                launch {
                    viewModel.signalEstimate.collect { estimate ->
                        if (estimate != null) {
                            binding.tvDirectionHint.text = estimate.message
                            
                            val color = when (estimate.trend) {
                                SignalTrend.IMPROVING -> ContextCompat.getColor(requireContext(), R.color.signal_excellent)
                                SignalTrend.WEAKENING -> ContextCompat.getColor(requireContext(), R.color.signal_poor)
                                SignalTrend.STABLE -> ContextCompat.getColor(requireContext(), R.color.signal_good)
                            }
                            binding.tvDirectionHint.setTextColor(color)
                        } else {
                            binding.tvDirectionHint.text = "Move around to detect signal direction"
                            binding.tvDirectionHint.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                        }
                    }
                }

                launch {
                    viewModel.nearbyNetworks.collect { networks ->
                        binding.tvNearbyCount.text = "${networks.size} nearby networks"
                    }
                }

                launch {
                    viewModel.isWifiEnabled.collect { enabled ->
                        if (!enabled) {
                            binding.tvNetworkName.text = "WiFi disabled"
                            binding.tvSignalStrength.text = "--"
                        }
                    }
                }
            }
        }
    }

    private fun updateSignalBars(quality: SignalQuality) {
        val activeColor = when (quality) {
            SignalQuality.EXCELLENT -> ContextCompat.getColor(requireContext(), R.color.signal_excellent)
            SignalQuality.GOOD -> ContextCompat.getColor(requireContext(), R.color.signal_good)
            SignalQuality.FAIR -> ContextCompat.getColor(requireContext(), R.color.signal_fair)
            SignalQuality.WEAK -> ContextCompat.getColor(requireContext(), R.color.signal_weak)
            SignalQuality.POOR -> ContextCompat.getColor(requireContext(), R.color.signal_poor)
        }
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.signal_inactive)

        val bars = listOf(
            binding.signalBar1,
            binding.signalBar2,
            binding.signalBar3,
            binding.signalBar4,
            binding.signalBar5
        )

        val activeBars = when (quality) {
            SignalQuality.EXCELLENT -> 5
            SignalQuality.GOOD -> 4
            SignalQuality.FAIR -> 3
            SignalQuality.WEAK -> 2
            SignalQuality.POOR -> 1
        }

        bars.forEachIndexed { index, bar ->
            bar.setBackgroundColor(if (index < activeBars) activeColor else inactiveColor)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
