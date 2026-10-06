package com.iocl.knowledgehub.ui.livenews

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.iocl.knowledgehub.data.repository.LiveNewsRepository
import com.iocl.knowledgehub.domain.model.LiveNewsItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

sealed interface LiveNewsDetailUiState {
    object Loading : LiveNewsDetailUiState
    data class Success(val article: LiveNewsItem) : LiveNewsDetailUiState
    data class Error(val message: String) : LiveNewsDetailUiState
}

@HiltViewModel
class LiveNewsDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val liveNewsRepository: LiveNewsRepository
) : ViewModel() {

    private val newsId: String = checkNotNull(savedStateHandle["newsId"]) {
        "newsId is required to launch LiveNewsDetailScreen"
    }

    private val _uiState = MutableStateFlow<LiveNewsDetailUiState>(LiveNewsDetailUiState.Loading)
    val uiState: StateFlow<LiveNewsDetailUiState> = _uiState.asStateFlow()

    init {
        loadArticleDetail()
    }

    private fun loadArticleDetail() {
        val article = liveNewsRepository.getNewsItemById(newsId)
        if (article != null) {
            _uiState.value = LiveNewsDetailUiState.Success(article)
        } else {
            _uiState.value = LiveNewsDetailUiState.Error("Article details not found. It might have been refreshed.")
        }
    }
}
