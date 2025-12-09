package com.example.androidapp.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.androidapp.AuthApplication
import com.example.androidapp.ChannelManager
import com.example.androidapp.R
import com.example.androidapp.databinding.FragmentSplashBinding
import com.example.androidapp.ui.FlutterDashboardActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        }, 2000)
    }

    private fun checkAuthenticationStatus() {
        lifecycleScope.launch {
            try {
                val isLoggedIn = tokenManager.isLoggedIn().first()

                if (!isAdded) return@launch

                if (isLoggedIn) {
                    Timber.d("User is logged in, navigating to Flutter Dashboard")
                    // User has valid session, launch Flutter Dashboard
                    launchFlutterDashboard()
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

    private fun launchFlutterDashboard() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                var email: String? = null
                var userId: String? = null
                var token: String? = null

                withContext(Dispatchers.IO) {
                    email = tokenManager.getUserEmail().first()
                    userId = tokenManager.getUserId().first()
                    token = tokenManager.getAccessToken().first()
                }

                if (email.isNullOrEmpty() || userId.isNullOrEmpty() || token.isNullOrEmpty()) {
                    Timber.e("Error: User session incomplete, navigating to login")
                    if (isAdded) {
                        findNavController().navigate(R.id.action_splashFragment_to_loginFragment)
                    }
                    return@launch
                }

                // Store session for Flutter usage
                ChannelManager.setUserSession(email, userId, token)
                Timber.d("User session set in ChannelManager")

                // Start Flutter Dashboard
                val intent = Intent(requireContext(), FlutterDashboardActivity::class.java).apply {
                    putExtra("email", email)
                    putExtra("userId", userId)
                    putExtra("token", token)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                requireActivity().finish()

            } catch (e: Exception) {
                Timber.e(e, "Error launching Flutter Dashboard")
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