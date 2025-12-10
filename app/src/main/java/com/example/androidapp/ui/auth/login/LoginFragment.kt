package com.example.androidapp.ui.auth.login

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.androidapp.AuthApplication
import com.example.androidapp.R
import com.example.androidapp.databinding.FragmentLoginBinding
import com.example.androidapp.ui.FlutterDashboardActivity
import com.example.androidapp.ui.factory.ViewModelFactory
import com.example.androidapp.ui.util.AnalyticsHelper
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private val authRepository by lazy {
        (requireActivity().application as AuthApplication).authRepository
    }

    private val viewModel: LoginViewModel by viewModels {
        ViewModelFactory(authRepository)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)

        // Initialize Firebase Analytics
        firebaseAnalytics = (requireActivity().application as AuthApplication).firebaseAnalytics

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Log screen view
        AnalyticsHelper.logScreenView(firebaseAnalytics, "LoginScreen")

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.loginButton.setOnClickListener {
            // Log button click
            AnalyticsHelper.logButtonClick(firebaseAnalytics, "login_button", "LoginScreen")

            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            viewModel.login(email, password)
        }

        binding.signupTextView.setOnClickListener {
            // Log navigation to signup
            AnalyticsHelper.logButtonClick(firebaseAnalytics, "signup_navigation", "LoginScreen")

            findNavController().navigate(R.id.action_loginFragment_to_signupFragment)
        }
    }

    private fun observeViewModel() {
        viewModel.loginResult.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LoginViewModel.LoginState.Idle -> {}
                is LoginViewModel.LoginState.Loading -> showLoading(true)
                is LoginViewModel.LoginState.Success -> {
                    showLoading(false)

                    // Log successful login
                    AnalyticsHelper.logLogin(firebaseAnalytics, "email")

                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    navigateToFlutterDashboard()
                }
                is LoginViewModel.LoginState.Error -> {
                    showLoading(false)

                    // Log login failure
                    AnalyticsHelper.logCustomEvent(
                        firebaseAnalytics,
                        "login_failed",
                        mapOf("error_message" to state.message)
                    )

                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.loginButton.isEnabled = !isLoading
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun navigateToFlutterDashboard() {
        val tokenManager = (requireActivity().application as AuthApplication).tokenManager

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
                    Toast.makeText(requireContext(), "Error: User session incomplete", Toast.LENGTH_LONG).show()
                    return@launch
                }

                // Log Flutter Dashboard navigation
                AnalyticsHelper.logCustomEvent(
                    firebaseAnalytics,
                    "flutter_dashboard_opened",
                    mapOf<String, Any>("user_id" to userId as Any)
                )

                // Store session for Flutter usage
                com.example.androidapp.ChannelManager.setUserSession(email!!, userId!!, token!!)

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
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
