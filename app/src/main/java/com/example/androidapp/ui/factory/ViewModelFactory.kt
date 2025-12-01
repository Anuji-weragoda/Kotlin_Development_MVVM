package com.example.androidapp.ui.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.androidapp.data.repository.AuthRepository
import com.example.androidapp.ui.auth.login.LoginViewModel
import com.example.androidapp.ui.auth.signup.SignupViewModel
import com.example.androidapp.ui.auth.signup.VerifyEmailViewModel

class ViewModelFactory(
    private val authRepository: AuthRepository,
    private val email: String? = null
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> {
                LoginViewModel(authRepository) as T
            }

            modelClass.isAssignableFrom(SignupViewModel::class.java) -> {
                SignupViewModel(authRepository) as T
            }

            modelClass.isAssignableFrom(VerifyEmailViewModel::class.java) -> {
                val email = email ?: throw IllegalArgumentException("Email required for VerifyEmailViewModel")
                VerifyEmailViewModel(authRepository, email) as T
            }

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
