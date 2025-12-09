package com.example.androidapp.ui.auth.signup

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
import com.example.androidapp.databinding.FragmentSignupBinding
import com.example.androidapp.ui.factory.ViewModelFactory
import com.example.androidapp.R
import com.example.androidapp.utils.AnalyticsHelper
import com.google.firebase.analytics.FirebaseAnalytics

class SignupFragment : Fragment() {

    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private val viewModel: SignupViewModel by viewModels {
        ViewModelFactory((requireActivity().application as AuthApplication).authRepository)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupBinding.inflate(inflater, container, false)

        // Initialize Firebase Analytics
        firebaseAnalytics = (requireActivity().application as AuthApplication).firebaseAnalytics

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Log screen view
        AnalyticsHelper.logScreenView(firebaseAnalytics, "SignupScreen")

        setupClickListeners()
        observeSignupState()
    }

    private fun setupClickListeners() {
        binding.signupButton.setOnClickListener {
            // Log signup button click
            AnalyticsHelper.logButtonClick(firebaseAnalytics, "signup_button", "SignupScreen")

            val fullName = binding.fullNameEditText.text.toString()
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            val confirmPassword = binding.confirmPasswordEditText.text.toString()

            viewModel.signup(fullName, email, password, confirmPassword)
        }

        binding.loginTextView.setOnClickListener {
            // Log navigation back to login
            AnalyticsHelper.logButtonClick(firebaseAnalytics, "back_to_login", "SignupScreen")

            findNavController().navigateUp()
        }
    }

    private fun observeSignupState() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.signupState.collect { state ->
                when (state) {
                    is SignupViewModel.SignupState.Idle -> {
                        showLoading(false)
                    }
                    is SignupViewModel.SignupState.Loading -> {
                        showLoading(true)
                    }
                    is SignupViewModel.SignupState.Success -> {
                        showLoading(false)

                        // Log successful signup
                        AnalyticsHelper.logSignUp(firebaseAnalytics, "email")

                        Toast.makeText(
                            requireContext(),
                            "Signup successful!",
                            Toast.LENGTH_SHORT
                        ).show()
                        val email = binding.emailEditText.text.toString()
                        val bundle = Bundle().apply {
                            putString("email", email)
                        }
                        findNavController().navigate(
                            R.id.action_signupFragment_to_verifyEmailFragment,
                            bundle
                        )

                    }
                    is SignupViewModel.SignupState.Error -> {
                        showLoading(false)

                        // Log signup failure
                        AnalyticsHelper.logCustomEvent(
                            firebaseAnalytics,
                            "signup_failed",
                            mapOf<String, Any>("error_message" to state.message as Any)
                        )

                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.signupButton.isEnabled = !isLoading
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
