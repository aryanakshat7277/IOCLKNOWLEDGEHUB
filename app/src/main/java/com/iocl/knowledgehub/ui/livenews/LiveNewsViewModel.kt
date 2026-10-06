package com.iocl.knowledgehub.ui.livenews

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iocl.knowledgehub.domain.model.LiveNewsCategory
import com.iocl.knowledgehub.domain.model.LiveNewsItem
import com.iocl.knowledgehub.data.repository.LiveNewsRepository
import com.iocl.knowledgehub.domain.model.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class LiveNewsUiState(
    val newsItems: List<LiveNewsItem> = emptyList(),
    val filteredNewsItems: List<LiveNewsItem> = emptyList(),
    val selectedCategory: LiveNewsCategory? = null,
    val selectedDateInMillis: Long = System.currentTimeMillis(),
    val isLatestNewsMode: Boolean = true,
    val lastUpdated: Long = 0L,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class LiveNewsViewModel @Inject constructor(
    private val liveNewsRepository: LiveNewsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveNewsUiState())
    val uiState: StateFlow<LiveNewsUiState> = _uiState.asStateFlow()

    private var allFetchedNews: List<LiveNewsItem> = emptyList()

    init {
        loadNews()
    }

    fun loadNews(forceRefresh: Boolean = false) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val dateStr = if (_uiState.value.isLatestNewsMode) "last 7 days" else formatDateString(_uiState.value.selectedDateInMillis)
            liveNewsRepository.getLiveNews(dateStr = dateStr, forceRefresh = forceRefresh).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        allFetchedNews = result.data
                        _uiState.update { state ->
                            state.copy(
                                newsItems = result.data,
                                filteredNewsItems = applyFilters(result.data, state.selectedDateInMillis, state.selectedCategory, state.isLatestNewsMode),
                                lastUpdated = System.currentTimeMillis(),
                                isLoading = false
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

    fun setLatestNewsMode(isLatest: Boolean) {
        _uiState.update { it.copy(isLatestNewsMode = isLatest, isLoading = true) }
        loadNews()
    }

    fun selectDate(millis: Long) {
        _uiState.update { it.copy(selectedDateInMillis = millis, isLatestNewsMode = false, isLoading = true) }
        viewModelScope.launch {
            val dateStr = formatDateString(millis)
            liveNewsRepository.getLiveNews(dateStr = dateStr, forceRefresh = false).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        allFetchedNews = result.data
                        _uiState.update { state ->
                            state.copy(
                                newsItems = result.data,
                                filteredNewsItems = applyFilters(result.data, millis, state.selectedCategory, false),
                                lastUpdated = System.currentTimeMillis(),
                                isLoading = false
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

    fun selectCategory(category: LiveNewsCategory?) {
        _uiState.update { state ->
            state.copy(
                selectedCategory = category,
                filteredNewsItems = applyFilters(allFetchedNews, state.selectedDateInMillis, category, state.isLatestNewsMode)
            )
        }
    }

    private fun applyFilters(
        items: List<LiveNewsItem>,
        dateInMillis: Long,
        category: LiveNewsCategory?,
        isLatestNewsMode: Boolean
    ): List<LiveNewsItem> {
        val dateFiltered = if (isLatestNewsMode) items else items.filter { isSameDay(it.publishedAt, dateInMillis) }
        return if (category == null) dateFiltered else dateFiltered.filter { it.category == category }
    }

    private fun formatDateString(millis: Long): String {
        return SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH).format(Date(millis))
    }

    private fun isSameDay(time1: Long, time2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
