package com.example.androidapp.ui.auth.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.data.repository.AuthRepository
import com.example.androidapp.ui.util.ValidationUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.androidapp.data.repository.Resource
import com.example.androidapp.data.remote.dto.AuthResponse

class SignupViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _signupState = MutableStateFlow<SignupState>(SignupState.Idle)
    val signupState: StateFlow<SignupState> = _signupState

    sealed class SignupState {
        object Idle : SignupState()
        object Loading : SignupState()
        data class Success(val email: String, val userConfirmed: Boolean) : SignupState()
        data class Error(val message: String) : SignupState()
    }

    fun signup(fullName: String, email: String, password: String, confirmPassword: String) {

        if (!ValidationUtil.isValidName(fullName)) {
            _signupState.value = SignupState.Error("Name must be at least 2 characters")
            return
        }

        if (!ValidationUtil.isValidEmail(email)) {
            _signupState.value = SignupState.Error("Invalid email format")
            return
        }

        if (!ValidationUtil.isValidPassword(password)) {
            _signupState.value = SignupState.Error("Password must be at least 6 characters")
            return
        }

        if (!ValidationUtil.doPasswordsMatch(password, confirmPassword)) {
            _signupState.value = SignupState.Error("Passwords do not match")
            return
        }

        viewModelScope.launch {
            authRepository.signup(email, password, fullName).collect { result ->
                when (result) {
                    is Resource.Loading -> _signupState.value = SignupState.Loading
                    is Resource.Success -> {
                        val authData: AuthResponse = result.data
                        _signupState.value = SignupState.Success(
                            email = email,
                            userConfirmed = authData.userConfirmed ?: false
                        )
                    }
                    is Resource.Error -> _signupState.value = SignupState.Error(result.message)
                }
            }
        }
    }

    fun resetState() {
        _signupState.value = SignupState.Idle
    }
}
