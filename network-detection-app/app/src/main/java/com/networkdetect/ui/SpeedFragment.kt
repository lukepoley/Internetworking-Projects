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
import com.networkdetect.databinding.FragmentSpeedBinding
import com.networkdetect.model.SpeedRating
import com.networkdetect.model.SpeedTestState
import kotlinx.coroutines.launch

class SpeedFragment : Fragment() {

    private var _binding: FragmentSpeedBinding? = null
    private val binding get() = _binding!!

    private val viewModel by lazy { (requireActivity() as MainActivity).viewModel }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSpeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnStartTest.setOnClickListener {
            viewModel.runSpeedTest()
        }

        observeData()
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.speedTestState.collect { state ->
                    when (state) {
                        is SpeedTestState.Idle -> {
                            showIdleState()
                        }
                        is SpeedTestState.Testing -> {
                            showTestingState(state.phase, state.progress)
                        }
                        is SpeedTestState.Complete -> {
                            showCompleteState(state.result)
                        }
                        is SpeedTestState.Error -> {
                            showErrorState(state.message)
                        }
                    }
                }
            }
        }
    }

    private fun showIdleState() {
        binding.btnStartTest.isEnabled = true
        binding.btnStartTest.text = "Start Speed Test"
        binding.progressTest.visibility = View.GONE
        binding.tvTestStatus.text = "Tap to test your connection speed"
        binding.layoutResults.visibility = View.GONE
    }

    private fun showTestingState(phase: String, progress: Int) {
        binding.btnStartTest.isEnabled = false
        binding.btnStartTest.text = "Testing..."
        binding.progressTest.visibility = View.VISIBLE
        binding.progressTest.progress = progress
        binding.tvTestStatus.text = phase
        binding.layoutResults.visibility = View.GONE
    }

    private fun showCompleteState(result: com.networkdetect.model.SpeedTestResult) {
        binding.btnStartTest.isEnabled = true
        binding.btnStartTest.text = "Test Again"
        binding.progressTest.visibility = View.GONE
        binding.layoutResults.visibility = View.VISIBLE

        // Display speeds
        binding.tvDownloadSpeed.text = String.format("%.1f", result.downloadSpeedMbps)
        binding.tvUploadSpeed.text = String.format("%.1f", result.uploadSpeedMbps)
        binding.tvLatency.text = if (result.latencyMs >= 0) "${result.latencyMs} ms" else "N/A"

        // Display rating
        binding.tvRating.text = result.speedRating.displayName
        binding.tvRatingDescription.text = result.speedRating.getDescription()

        val ratingColor = when (result.speedRating) {
            SpeedRating.VERY_FAST -> ContextCompat.getColor(requireContext(), R.color.signal_excellent)
            SpeedRating.FAST -> ContextCompat.getColor(requireContext(), R.color.signal_good)
            SpeedRating.MODERATE -> ContextCompat.getColor(requireContext(), R.color.signal_fair)
            SpeedRating.SLOW -> ContextCompat.getColor(requireContext(), R.color.signal_weak)
            SpeedRating.VERY_SLOW -> ContextCompat.getColor(requireContext(), R.color.signal_poor)
        }
        binding.tvRating.setTextColor(ratingColor)
        binding.cardRating.strokeColor = ratingColor

        binding.tvTestStatus.text = "Test completed"
    }

    private fun showErrorState(message: String) {
        binding.btnStartTest.isEnabled = true
        binding.btnStartTest.text = "Retry Test"
        binding.progressTest.visibility = View.GONE
        binding.tvTestStatus.text = "Error: $message"
        binding.layoutResults.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
