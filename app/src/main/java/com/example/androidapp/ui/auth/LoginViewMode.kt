package com.example.androidapp.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.androidapp.ui.util.ValidationUtil


class LoginViewModel : ViewModel() {

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

        _loginResult.value = LoginState.Success("Login successful!")
    }

    fun resetState() {
        _loginResult.value = LoginState.Idle
    }


}