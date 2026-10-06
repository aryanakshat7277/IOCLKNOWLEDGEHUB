package com.iocl.knowledgehub.domain.usecase.newspaper

import com.iocl.knowledgehub.domain.model.Newspaper
import com.iocl.knowledgehub.domain.model.Resource
import com.iocl.knowledgehub.domain.repository.NewspaperRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetNewspapersUseCase @Inject constructor(
    private val newspaperRepository: NewspaperRepository
) {
    operator fun invoke(): Flow<Resource<List<Newspaper>>> = newspaperRepository.getNewspapers()
    fun byDateRange(startDate: Long, endDate: Long): Flow<Resource<List<Newspaper>>> = newspaperRepository.getNewspapersByDateRange(startDate, endDate)
}
