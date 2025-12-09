package com.example.androidapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.androidapp.AuthApplication
import com.example.androidapp.databinding.FragmentHomeBinding
import com.example.androidapp.R
import kotlinx.coroutines.launch
import timber.log.Timber


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val tokenManager by lazy {
        (requireActivity().application as AuthApplication).tokenManager
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Logout button
        binding.logoutButton.setOnClickListener {
            logout()
        }
    }

    private fun logout() {
        lifecycleScope.launch {
            try {
                // Clear all tokens and user data
                tokenManager.clearTokens()
                Timber.d("User logged out successfully")

                // Navigate to LoginFragment and clear back stack
                findNavController().navigate(
                    R.id.action_homeFragment_to_loginFragment
                )
            } catch (e: Exception) {
                Timber.e(e, "Error during logout")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
