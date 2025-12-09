package com.example.androidapp.ui.splash

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.androidapp.AuthApplication
import com.example.androidapp.R
import com.example.androidapp.databinding.FragmentSplashBinding
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber


class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    private val tokenManager by lazy {
        (requireActivity().application as AuthApplication).tokenManager
    }

    private val handler = Handler(Looper.getMainLooper())

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

        // Add fade-in animation
        binding.root.alpha = 0f
        binding.root.animate()
            .alpha(1f)
            .setDuration(1000)
            .start()

        // Check authentication status after delay
        handler.postDelayed({
            checkAuthenticationStatus()
        }, 2000) // Reduced delay for better UX
    }

    private fun checkAuthenticationStatus() {
        lifecycleScope.launch {
            try {
                val isLoggedIn = tokenManager.isLoggedIn().first()

                if (!isAdded) return@launch

                if (isLoggedIn) {
                    Timber.d("User is logged in, navigating to home")
                    // User has valid session, go to home
                    findNavController().navigate(R.id.action_splashFragment_to_homeFragment)
                } else {
                    Timber.d("User not logged in, navigating to login")
                    // No valid session, go to login
                    findNavController().navigate(R.id.action_splashFragment_to_loginFragment)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error checking authentication status")
                // On error, navigate to login for safety
                if (isAdded) {
                    findNavController().navigate(R.id.action_splashFragment_to_loginFragment)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
        _binding = null
    }
}