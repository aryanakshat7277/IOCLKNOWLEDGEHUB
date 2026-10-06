package com.iocl.knowledgehub.ui.newspaper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iocl.knowledgehub.domain.model.*
import com.iocl.knowledgehub.domain.usecase.bookmark.ToggleBookmarkUseCase
import com.iocl.knowledgehub.domain.usecase.newspaper.GetNewspapersUseCase
import com.iocl.knowledgehub.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject


data class NewspaperUiState(
    val newspapers: List<Newspaper> = emptyList(),
    val selectedFilter: String = "All",
    val isLoading: Boolean = true,
    val isAdmin: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class NewspaperViewModel @Inject constructor(
    private val getNewspapersUseCase: GetNewspapersUseCase,
    private val toggleBookmarkUseCase: ToggleBookmarkUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewspaperUiState())
    val uiState: StateFlow<NewspaperUiState> = _uiState.asStateFlow()

    init {
        loadNewspapers()
        val role = authRepository.getSavedRole()
        val isAdmin = role == UserRole.ADMIN || role == UserRole.SUPER_ADMIN
        _uiState.update { it.copy(isAdmin = isAdmin) }
    }

    private fun loadNewspapers() {
        viewModelScope.launch {
            getNewspapersUseCase().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val filtered = filterNewspapers(result.data, _uiState.value.selectedFilter)
                        _uiState.update {
                            it.copy(
                                newspapers = filtered,
                                isLoading = false,
                                error = null
                            )
                        }
                    }

                    is Resource.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = result.message) }
                    }

                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }



    fun onFilterSelected(filter: String) {
        _uiState.update { it.copy(selectedFilter = filter) }
        loadNewspapers()
    }

    fun toggleBookmark(newspaper: Newspaper) {
        viewModelScope.launch {
            toggleBookmarkUseCase(
                Bookmark(
                    documentId = newspaper.id,
                    documentType = DocumentType.NEWSPAPER,
                    title = newspaper.title,
                    coverImageUrl = newspaper.coverImageUrl
                )
            )
        }
    }

    fun refresh() {
        loadNewspapers()
    }

    private fun filterNewspapers(papers: List<Newspaper>, filter: String): List<Newspaper> {
        val now = System.currentTimeMillis()
        val day = 86_400_000L
        return when (filter) {
            "Today" -> papers.filter { now - it.publishDate < day }
            "This Week" -> papers.filter { now - it.publishDate < 7 * day }
            "This Month" -> papers.filter { now - it.publishDate < 30 * day }
            else -> papers
        }
    }
}
