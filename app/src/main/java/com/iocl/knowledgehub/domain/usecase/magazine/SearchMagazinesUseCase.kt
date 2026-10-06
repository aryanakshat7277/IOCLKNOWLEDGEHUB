package com.iocl.knowledgehub.domain.usecase.magazine

import com.iocl.knowledgehub.domain.model.Magazine
import com.iocl.knowledgehub.domain.model.Resource
import com.iocl.knowledgehub.domain.repository.MagazineRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchMagazinesUseCase @Inject constructor(
    private val magazineRepository: MagazineRepository
) {
    operator fun invoke(query: String): Flow<Resource<List<Magazine>>> {
        return magazineRepository.searchMagazines(query.trim())
    }
}
