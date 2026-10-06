package com.iocl.knowledgehub.domain.usecase.bookmark

import com.iocl.knowledgehub.domain.model.Bookmark
import com.iocl.knowledgehub.domain.model.Resource
import com.iocl.knowledgehub.domain.repository.BookmarkRepository
import javax.inject.Inject

class ToggleBookmarkUseCase @Inject constructor(
    private val bookmarkRepository: BookmarkRepository
) {
    suspend operator fun invoke(bookmark: Bookmark): Resource<Boolean> {
        return bookmarkRepository.toggleBookmark(bookmark)
    }
}
