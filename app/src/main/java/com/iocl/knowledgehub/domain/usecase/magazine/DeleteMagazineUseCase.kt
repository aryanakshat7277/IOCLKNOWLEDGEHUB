package com.iocl.knowledgehub.domain.usecase.magazine

import com.iocl.knowledgehub.domain.model.Resource
import com.iocl.knowledgehub.domain.repository.MagazineRepository
import javax.inject.Inject

class DeleteMagazineUseCase @Inject constructor(
    private val magazineRepository: MagazineRepository
) {
    suspend operator fun invoke(id: String): Resource<Unit> {
        if (id.isBlank()) return Resource.Error("Invalid magazine ID")
        return magazineRepository.deleteMagazine(id)
    }
}
