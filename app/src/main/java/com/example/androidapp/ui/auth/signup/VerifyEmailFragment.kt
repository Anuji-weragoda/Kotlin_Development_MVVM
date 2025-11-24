package com.example.androidapp.ui.auth.signup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.androidapp.databinding.FragmentVerifyEmailBinding

class VerifyEmailFragment : Fragment() {

    private var _binding: FragmentVerifyEmailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VerifyEmailViewModel by viewModels()

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
            val code = binding.codeEditText.text.toString()
            if (code.isNotEmpty()) {
                viewModel.verifyCode(code)
            } else {
                Toast.makeText(requireContext(), "Please enter the code", Toast.LENGTH_SHORT).show()
            }
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.verifyState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is VerifyEmailViewModel.VerifyState.Loading -> {
                    binding.verifyButton.isEnabled = false
                }
                is VerifyEmailViewModel.VerifyState.Success -> {
                    binding.verifyButton.isEnabled = true
                    Toast.makeText(requireContext(), "Email verified!", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp() // Go to login page
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
