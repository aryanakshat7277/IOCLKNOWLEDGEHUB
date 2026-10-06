package com.iocl.knowledgehub.domain.usecase.magazine

import com.iocl.knowledgehub.domain.model.Magazine
import com.iocl.knowledgehub.domain.model.Resource
import com.iocl.knowledgehub.domain.repository.MagazineRepository
import javax.inject.Inject

class UploadMagazineUseCase @Inject constructor(
    private val magazineRepository: MagazineRepository
) {
    suspend operator fun invoke(magazine: Magazine, pdfBytes: ByteArray, coverBytes: ByteArray?): Resource<Magazine> {
        if (magazine.title.isBlank()) return Resource.Error("Title is required")
        if (pdfBytes.isEmpty()) return Resource.Error("PDF file is required")
        return magazineRepository.uploadMagazine(magazine, pdfBytes, coverBytes)
    }
}
