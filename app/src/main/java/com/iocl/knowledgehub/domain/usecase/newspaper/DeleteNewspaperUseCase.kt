package com.iocl.knowledgehub.domain.usecase.newspaper

import com.iocl.knowledgehub.domain.model.Resource
import com.iocl.knowledgehub.domain.repository.NewspaperRepository
import javax.inject.Inject

class DeleteNewspaperUseCase @Inject constructor(
    private val newspaperRepository: NewspaperRepository
) {
    suspend operator fun invoke(id: String): Resource<Unit> {
        if (id.isBlank()) return Resource.Error("Invalid newspaper ID")
        return newspaperRepository.deleteNewspaper(id)
    }
}
