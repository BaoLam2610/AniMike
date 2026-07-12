package com.lambao.animike.ui.debug

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.lambao.animike.data.repository.DebugRepository
import com.lambao.animike.ui.base.BaseViewModel
import com.lambao.animike.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DebugTableDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val debugRepository: DebugRepository,
) : BaseViewModel<DebugTableDetailState, DebugTableDetailEvent, DebugTableDetailEffect>(
    DebugTableDetailState(),
) {

    private val tableName: String = checkNotNull(savedStateHandle[Routes.DEBUG_TABLE_DETAIL_ARG_NAME])
    private var loadJob: Job? = null

    init {
        setState { copy(tableName = tableName) }
        load()
    }

    override fun onEvent(event: DebugTableDetailEvent) {
        when (event) {
            DebugTableDetailEvent.OnRetry -> load()
            is DebugTableDetailEvent.OnRowLimitChanged -> {
                setState { copy(rowLimit = event.limit) }
                load()
            }
        }
    }

    private fun load() {
        // Hủy lần load trước để retry/đổi limit dồn không tạo coroutine chồng
        // nhau (VD user bấm liên tiếp "Tất cả" rồi "20").
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            try {
                val detail = debugRepository.tableDetail(tableName, currentState().rowLimit)
                setState { copy(detail = detail) }
            } catch (e: Exception) {
                setState { copy(error = e.message ?: "Lỗi đọc bảng") }
            } finally {
                setState { copy(isLoading = false) }
            }
        }
    }
}
