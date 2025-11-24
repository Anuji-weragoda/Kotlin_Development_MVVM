package com.example.androidapp.ui.auth.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.data.repository.AuthRepository
import com.example.androidapp.data.repository.Resource
import com.example.androidapp.ui.util.ValidationUtil
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _loginResult = MutableLiveData<LoginState>()
    val loginResult: LiveData<LoginState> = _loginResult

    sealed class LoginState {
        object Idle : LoginState()
        object Loading : LoginState()
        data class Success(val message: String) : LoginState()
        data class Error(val message: String) : LoginState()
    }

    fun login(email: String, password: String) {
        _loginResult.value = LoginState.Loading

        if (!ValidationUtil.isValidEmail(email)) {
            _loginResult.value = LoginState.Error("Invalid email format")
            return
        }

        if (!ValidationUtil.isValidPassword(password)) {
            _loginResult.value = LoginState.Error("Password must be at least 6 characters")
            return
        }

        viewModelScope.launch {
            authRepository.login(email, password).collectLatest { resource ->
                when (resource) {
                    is Resource.Loading -> _loginResult.value = LoginState.Loading
                    is Resource.Success -> _loginResult.value =
                        LoginState.Success("Welcome back, ${resource.data.userAttributes?.email ?: "user"}!")
                    is Resource.Error -> _loginResult.value =
                        LoginState.Error(resource.message ?: "Login failed")
                }
            }
        }
    }

    fun resetState() {
        _loginResult.value = LoginState.Idle
    }
}
