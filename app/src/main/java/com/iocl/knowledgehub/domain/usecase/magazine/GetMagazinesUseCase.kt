package com.iocl.knowledgehub.domain.usecase.magazine

import com.iocl.knowledgehub.domain.model.Magazine
import com.iocl.knowledgehub.domain.model.Resource
import com.iocl.knowledgehub.domain.repository.MagazineRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMagazinesUseCase @Inject constructor(
    private val magazineRepository: MagazineRepository
) {
    operator fun invoke(): Flow<Resource<List<Magazine>>> = magazineRepository.getMagazines()
    fun byYear(year: Int): Flow<Resource<List<Magazine>>> = magazineRepository.getMagazinesByYear(year)
    fun byMonthAndYear(month: Int, year: Int): Flow<Resource<List<Magazine>>> = magazineRepository.getMagazinesByMonthAndYear(month, year)
}
