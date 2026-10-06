package com.iocl.knowledgehub.domain.usecase.bookmark

import com.iocl.knowledgehub.domain.model.Bookmark
import com.iocl.knowledgehub.domain.model.Resource
import com.iocl.knowledgehub.domain.repository.BookmarkRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBookmarksUseCase @Inject constructor(
    private val bookmarkRepository: BookmarkRepository
) {
    operator fun invoke(): Flow<Resource<List<Bookmark>>> = bookmarkRepository.getBookmarks()
}
