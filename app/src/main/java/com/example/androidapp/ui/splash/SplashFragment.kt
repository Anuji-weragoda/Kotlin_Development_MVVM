package com.example.androidapp.ui.splash

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.androidapp.R
import com.example.androidapp.databinding.FragmentSplashBinding
import androidx.navigation.fragment.findNavController


class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!


    private val handler = Handler(Looper.getMainLooper())


    private val navigateToLogin = Runnable {

        if (isAdded) {
            findNavController().navigate(R.id.action_splashFragment_to_loginFragment)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Optional: Add fade-in animation
        binding.root.alpha = 0f
        binding.root.animate()
            .alpha(1f)
            .setDuration(1000)
            .start()

        handler.postDelayed(navigateToLogin, 3500)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(navigateToLogin)
        _binding = null
    }
}