package com.iocl.knowledgehub.domain.usecase.newspaper

import com.iocl.knowledgehub.domain.model.Newspaper
import com.iocl.knowledgehub.domain.model.Resource
import com.iocl.knowledgehub.domain.repository.NewspaperRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchNewspapersUseCase @Inject constructor(
    private val newspaperRepository: NewspaperRepository
) {
    operator fun invoke(query: String): Flow<Resource<List<Newspaper>>> {
        return newspaperRepository.searchNewspapers(query.trim())
    }
}
