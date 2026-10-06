package com.iocl.knowledgehub.domain.usecase.auth

import com.iocl.knowledgehub.domain.model.Resource
import com.iocl.knowledgehub.domain.model.User
import com.iocl.knowledgehub.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<User?> = authRepository.currentUser
    suspend fun get(): Resource<User> = authRepository.getCurrentUser()
}
