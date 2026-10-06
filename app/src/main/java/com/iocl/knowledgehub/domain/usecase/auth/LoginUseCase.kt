package com.iocl.knowledgehub.domain.usecase.auth

import com.iocl.knowledgehub.domain.model.Resource
import com.iocl.knowledgehub.domain.model.User
import com.iocl.knowledgehub.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Resource<User> {
        if (email.isBlank()) return Resource.Error("Email cannot be empty")
        if (password.isBlank()) return Resource.Error("Password cannot be empty")
        if (!email.contains("@")) return Resource.Error("Invalid email format")
        if (password.length < 6) return Resource.Error("Password must be at least 6 characters")
        return authRepository.login(email, password)
    }
}
