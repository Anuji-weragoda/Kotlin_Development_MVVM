package com.example.androidapp.ui.auth.signup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.data.repository.AuthRepository
import kotlinx.coroutines.launch

class VerifyEmailViewModel(
    private val authRepository: AuthRepository,
    private val email: String
) : ViewModel() {

    private val _verifyState = MutableLiveData<VerifyState>()
    val verifyState: LiveData<VerifyState> = _verifyState

    sealed class VerifyState {
        object Loading : VerifyState()
        object Success : VerifyState()
        data class Error(val message: String) : VerifyState()
    }

    fun verifyCode(code: String) {
        _verifyState.value = VerifyState.Loading

        viewModelScope.launch {
            try {
                val result = authRepository.confirmSignup(email, code)
                if (result) {
                    _verifyState.value = VerifyState.Success
                } else {
                    _verifyState.value = VerifyState.Error("Verification failed")
                }
            } catch (e: Exception) {
                _verifyState.value = VerifyState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }
}
