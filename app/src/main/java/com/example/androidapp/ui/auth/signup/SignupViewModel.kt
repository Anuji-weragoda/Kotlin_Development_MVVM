package com.example.androidapp.ui.auth.signup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.androidapp.ui.util.ValidationUtil

class SignupViewModel : ViewModel() {
    private val _signupResult = MutableLiveData<SignupState>()
    val signupResult: LiveData<SignupState> = _signupResult

    sealed class SignupState {
        object Idle : SignupState()
        object Loading : SignupState()
        data class Success(val message: String) : SignupState()
        data class Error(val message: String) : SignupState()
    }

    fun signup(fullName: String, email: String, password: String, confirmPassword: String) {
        _signupResult.value = SignupState.Loading


        if (!ValidationUtil.isValidName(fullName)) {
            _signupResult.value = SignupState.Error("Name must be at least 2 characters")
            return
        }


        if (!ValidationUtil.isValidEmail(email)) {
            _signupResult.value = SignupState.Error("Invalid email format")
            return
        }

        if (!ValidationUtil.isValidPassword(password)) {
            _signupResult.value = SignupState.Error("Password must be at least 6 characters")
            return
        }

        if (!ValidationUtil.doPasswordsMatch(password, confirmPassword)) {
            _signupResult.value = SignupState.Error("Passwords do not match")
            return
        }


        _signupResult.value = SignupState.Success("Account created successfully!")
    }

    fun resetState() {
        _signupResult.value = SignupState.Idle
    }

}