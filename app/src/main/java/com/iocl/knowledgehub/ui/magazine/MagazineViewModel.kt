package com.iocl.knowledgehub.ui.magazine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iocl.knowledgehub.domain.model.*
import com.iocl.knowledgehub.domain.usecase.bookmark.ToggleBookmarkUseCase
import com.iocl.knowledgehub.domain.usecase.magazine.GetMagazinesUseCase
import com.iocl.knowledgehub.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MagazineUiState(
    val magazines: List<Magazine> = emptyList(),
    val selectedYear: String = "2026",
    val selectedMonth: String = "All",
    val isLoading: Boolean = true,
    val isAdmin: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MagazineViewModel @Inject constructor(
    private val getMagazinesUseCase: GetMagazinesUseCase,
    private val toggleBookmarkUseCase: ToggleBookmarkUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MagazineUiState())
    val uiState: StateFlow<MagazineUiState> = _uiState.asStateFlow()

    init {
        loadMagazines()
        val role = authRepository.getSavedRole()
        val isAdmin = role == UserRole.ADMIN || role == UserRole.SUPER_ADMIN
        _uiState.update { it.copy(isAdmin = isAdmin) }
    }

    private fun loadMagazines() {
        viewModelScope.launch {
            val year = _uiState.value.selectedYear.toIntOrNull() ?: 2026
            getMagazinesUseCase.byYear(year).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val filtered = if (_uiState.value.selectedMonth == "All") {
                            result.data
                        } else {
                            result.data.filter { it.month.toMonthName() == _uiState.value.selectedMonth }
                        }
                        _uiState.update {
                            it.copy(
                                magazines = filtered,
                                isLoading = false,
                                error = null
                            )
                        }
                    }

                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(isLoading = false, error = result.message)
                        }
                    }

                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    fun onYearSelected(year: String) {
        _uiState.update { it.copy(selectedYear = year, selectedMonth = "All") }
        loadMagazines()
    }

    fun onMonthSelected(month: String) {
        _uiState.update { it.copy(selectedMonth = month) }
        loadMagazines()
    }

    fun toggleBookmark(magazine: Magazine) {
        viewModelScope.launch {
            toggleBookmarkUseCase(
                Bookmark(
                    documentId = magazine.id,
                    documentType = DocumentType.MAGAZINE,
                    title = magazine.title,
                    coverImageUrl = magazine.coverImageUrl
                )
            )
        }
    }

    fun refresh() {
        loadMagazines()
    }
}
