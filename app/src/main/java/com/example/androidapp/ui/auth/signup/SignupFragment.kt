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
import kotlinx.coroutines.flow.collect

class SignupFragment : Fragment() {

    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SignupViewModel by viewModels {
        ViewModelFactory((requireActivity().application as AuthApplication).authRepository)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeSignupState()
    }

    private fun setupClickListeners() {
        binding.signupButton.setOnClickListener {
            val fullName = binding.fullNameEditText.text.toString()
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            val confirmPassword = binding.confirmPasswordEditText.text.toString()

            viewModel.signup(fullName, email, password, confirmPassword)
        }

        binding.loginTextView.setOnClickListener {
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
                        Toast.makeText(
                            requireContext(),
                            "Signup successful!",
                            Toast.LENGTH_SHORT
                        ).show()
                        findNavController().navigateUp()
                    }
                    is SignupViewModel.SignupState.Error -> {
                        showLoading(false)
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
