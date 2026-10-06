package com.iocl.knowledgehub.domain.usecase.auth

import com.iocl.knowledgehub.domain.model.Resource
import com.iocl.knowledgehub.domain.repository.AuthRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Resource<Unit> = authRepository.logout()
}
