package com.example.androidapp.ui.auth.signup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.androidapp.AuthApplication
import com.example.androidapp.R
import com.example.androidapp.databinding.FragmentVerifyEmailBinding
import com.example.androidapp.ui.factory.ViewModelFactory

class VerifyEmailFragment : Fragment() {

    private var _binding: FragmentVerifyEmailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VerifyEmailViewModel by viewModels {
        val emailArg = VerifyEmailFragmentArgs.fromBundle(requireArguments()).email
        ViewModelFactory(
            (requireActivity().application as AuthApplication).authRepository,
            emailArg
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVerifyEmailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.verifyButton.setOnClickListener {
            val code = binding.codeEditText.text.toString().trim()

            if (code.isNotEmpty()) {
                viewModel.verifyCode(code)
            } else {
                Toast.makeText(requireContext(), "Please enter the code", Toast.LENGTH_SHORT).show()
            }
        }

        observeViewModel()
    }

    private fun observeViewModel() {

        // Observe verification state
        viewModel.verifyState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is VerifyEmailViewModel.VerifyState.Loading -> {
                    binding.verifyButton.isEnabled = false
                }

                is VerifyEmailViewModel.VerifyState.Success -> {
                    binding.verifyButton.isEnabled = true
                    Toast.makeText(requireContext(), "Email verified! Please login to continue.", Toast.LENGTH_LONG).show()

                    // Navigate to Login screen (user needs to login after verification)
                    // Tokens are not saved during signup, only during login
                    findNavController().navigate(R.id.action_verifyEmailFragment_to_loginFragment)
                }

                is VerifyEmailViewModel.VerifyState.Error -> {
                    binding.verifyButton.isEnabled = true
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
