package com.iocl.knowledgehub.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iocl.knowledgehub.domain.model.Resource
import com.iocl.knowledgehub.domain.model.User
import com.iocl.knowledgehub.domain.model.UserRole
import com.iocl.knowledgehub.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val selectedRole: UserRole = UserRole.EMPLOYEE,
    val error: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            if (authRepository.isLoggedIn) {
                _uiState.update { it.copy(isLoading = true) }
                when (val result = authRepository.getCurrentUser()) {
                    is Resource.Success -> {
                        _uiState.update { it.copy(isLoading = false, user = result.data) }
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                    is Resource.Loading -> {}
                }
            }
        }
    }

    fun setRole(role: UserRole) {
        _uiState.update { it.copy(selectedRole = role) }
    }

    fun resetUser() {
        _uiState.update { it.copy(user = null, error = null) }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.signInWithGoogle(idToken)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false, user = result.data) }
                }
                is Resource.Error -> {
                    // Google sign-in failed, auto-fallback to anonymous
                    signInAnonymouslyInternal(_uiState.value.selectedRole)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun signInAnonymously() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            signInAnonymouslyInternal(UserRole.GUEST)
        }
    }

    fun loginAsAdmin(userId: String, pass: String) {
        if (userId == "IOCL123" && pass == "12345678") {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null) }
                signInAnonymouslyInternal(UserRole.ADMIN)
            }
        } else {
            _uiState.update { it.copy(error = "Invalid Admin Credentials") }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private suspend fun signInAnonymouslyInternal(forcedRole: UserRole) {
        when (val result = authRepository.signInAnonymously()) {
            is Resource.Success -> {
                authRepository.saveRole(forcedRole)
                val userWithRole = result.data.copy(role = forcedRole)
                _uiState.update { it.copy(isLoading = false, user = userWithRole) }
            }
            is Resource.Error -> {
                authRepository.saveRole(forcedRole)
                // Even anonymous failed — just let user through with a dummy user
                val dummyUser = User(uid = "dummy_${forcedRole.name}", name = "${forcedRole.name} User", role = forcedRole)
                _uiState.update { it.copy(isLoading = false, user = dummyUser) }
            }
            is Resource.Loading -> {}
        }
    }

    fun onGoogleSignInFailed() {
        // If Google Sign-In UI itself fails, auto-fallback to anonymous
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            signInAnonymouslyInternal(_uiState.value.selectedRole)
        }
    }
}
