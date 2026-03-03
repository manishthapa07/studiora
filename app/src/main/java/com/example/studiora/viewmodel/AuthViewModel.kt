package com.example.studiora.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studiora.model.User
import com.example.studiora.repository.AuthRepository
import com.example.studiora.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}

sealed class ResetState {
    object Idle : ResetState()
    object Loading : ResetState()
    object Success : ResetState()
    data class Error(val message: String) : ResetState()
}

sealed class RegisterState {
    object Idle : RegisterState()
    object Loading : RegisterState()
    data class Success(val user: User) : RegisterState()
    data class Error(val message: String) : RegisterState()
}

sealed class ProfileUpdateState {
    object Idle : ProfileUpdateState()
    object Loading : ProfileUpdateState()
    object Success : ProfileUpdateState()
    data class Error(val message: String) : ProfileUpdateState()
}

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()
    private val userRepository = UserRepository()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _resetState = MutableStateFlow<ResetState>(ResetState.Idle)
    val resetState: StateFlow<ResetState> = _resetState.asStateFlow()

    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registerState: StateFlow<RegisterState> = _registerState.asStateFlow()

    private val _currentUserData = MutableStateFlow<User?>(null)
    val currentUserData: StateFlow<User?> = _currentUserData.asStateFlow()

    private val _profileUpdateState = MutableStateFlow<ProfileUpdateState>(ProfileUpdateState.Idle)
    val profileUpdateState: StateFlow<ProfileUpdateState> = _profileUpdateState.asStateFlow()

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.signIn(email, password)
            _authState.value = result.fold(
                onSuccess = {
                    _currentUserData.value = it
                    AuthState.Success(it)
                },
                onFailure = { AuthState.Error(it.message ?: "Login failed") }
            )
        }
    }

    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            _resetState.value = ResetState.Loading
            val result = repository.sendPasswordReset(email)
            _resetState.value = result.fold(
                onSuccess = { ResetState.Success },
                onFailure = { ResetState.Error(it.message ?: "Failed to send reset email") }
            )
        }
    }

    fun loadCurrentUser() {
        viewModelScope.launch {
            val uid = repository.getCurrentUser()?.uid ?: return@launch
            val result = repository.getUserById(uid)
            result.onSuccess { _currentUserData.value = it }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun resetPasswordState() {
        _resetState.value = ResetState.Idle
    }

    fun registerOrganization(orgName: String, email: String, password: String, phone: String) {
        viewModelScope.launch {
            _registerState.value = RegisterState.Loading
            val result = repository.registerOrganization(orgName, email, password, phone)
            _registerState.value = result.fold(
                onSuccess = {
                    _currentUserData.value = it
                    RegisterState.Success(it)
                },
                onFailure = { RegisterState.Error(it.message ?: "Registration failed") }
            )
        }
    }

    fun resetRegisterState() {
        _registerState.value = RegisterState.Idle
    }

    fun updateProfile(updatedUser: User) {
        viewModelScope.launch {
            _profileUpdateState.value = ProfileUpdateState.Loading
            val result = userRepository.updateUser(updatedUser)
            _profileUpdateState.value = result.fold(
                onSuccess = {
                    _currentUserData.value = updatedUser
                    ProfileUpdateState.Success
                },
                onFailure = { ProfileUpdateState.Error(it.message ?: "Failed to update profile") }
            )
        }
    }

    fun resetProfileUpdateState() {
        _profileUpdateState.value = ProfileUpdateState.Idle
    }

    fun getCurrentUser() = repository.getCurrentUser()

    fun signOut() {
        repository.signOut()
        _currentUserData.value = null
        _authState.value = AuthState.Idle
    }
}
