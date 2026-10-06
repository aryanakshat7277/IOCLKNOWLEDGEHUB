package com.iocl.knowledgehub.domain.usecase.newspaper

import com.iocl.knowledgehub.domain.model.Newspaper
import com.iocl.knowledgehub.domain.model.Resource
import com.iocl.knowledgehub.domain.repository.NewspaperRepository
import javax.inject.Inject

class UploadNewspaperUseCase @Inject constructor(
    private val newspaperRepository: NewspaperRepository
) {
    suspend operator fun invoke(newspaper: Newspaper, pdfBytes: ByteArray, coverBytes: ByteArray?): Resource<Newspaper> {
        if (newspaper.title.isBlank()) return Resource.Error("Title is required")
        if (pdfBytes.isEmpty()) return Resource.Error("PDF file is required")
        return newspaperRepository.uploadNewspaper(newspaper, pdfBytes, coverBytes)
    }
}
