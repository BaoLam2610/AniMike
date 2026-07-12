package com.lambao.animike.ui.debug

import androidx.lifecycle.viewModelScope
import com.lambao.animike.data.repository.DebugRepository
import com.lambao.animike.debug.DebugInspector
import com.lambao.animike.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DebugViewModel @Inject constructor(
    private val debugRepository: DebugRepository,
) : BaseViewModel<DebugState, DebugEvent, DebugEffect>(DebugState()) {

    init {
        // 2 ring-buffer của DebugInspector là StateFlow global — mirror thẳng
        // vào state để UI collect qua 1 nguồn duy nhất (state), đồng nhất MVI.
        DebugInspector.networkLogs
            .onEach { logs -> setState { copy(networkLogs = logs) } }
            .launchIn(viewModelScope)
        DebugInspector.appLogs
            .onEach { logs -> setState { copy(appLogs = logs) } }
            .launchIn(viewModelScope)
        loadStats()
    }

    override fun onEvent(event: DebugEvent) {
        when (event) {
            is DebugEvent.OnTabSelected -> setState { copy(selectedTab = event.tab) }

            DebugEvent.OnClearNetworkLogs -> DebugInspector.clearNetworkLogs()

            DebugEvent.OnClearAppLogs -> DebugInspector.clearAppLogs()

            DebugEvent.OnRefreshStats -> loadStats()

            DebugEvent.OnClearCache -> viewModelScope.launch {
                debugRepository.clearCacheTables()
                loadStats()
                sendEffect(DebugEffect.ShowMessage("Đã xoá cache (giữ favorite/tracking)"))
            }

            is DebugEvent.OnClearTable -> viewModelScope.launch {
                debugRepository.clearTable(event.name)
                loadStats()
                sendEffect(DebugEffect.ShowMessage("Đã xoá bảng ${event.name}"))
            }

            is DebugEvent.OnNetworkQueryChange -> setState { copy(networkQuery = event.query) }

            is DebugEvent.OnLogQueryChange -> setState { copy(logQuery = event.query) }

            is DebugEvent.OnCacheQueryChange -> setState { copy(cacheQuery = event.query) }

            is DebugEvent.OnNetworkLogClick -> sendEffect(DebugEffect.NavigateToNetworkDetail(event.id))

            is DebugEvent.OnTableClick -> sendEffect(DebugEffect.NavigateToTableDetail(event.name))
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            setState { copy(isLoadingStats = true) }
            try {
                // Truy vấn raw SQL trên writableDatabase có thể ném (bảng lạ,
                // DB lỗi) — không để coroutine chết ngầm khiến isLoadingStats
                // kẹt true mãi; báo lỗi qua effect + reset cờ ở finally.
                val stats = debugRepository.tableStats()
                setState { copy(tableStats = stats) }
            } catch (e: Exception) {
                sendEffect(DebugEffect.ShowMessage("Lỗi đọc thống kê bảng: ${e.message}"))
            } finally {
                setState { copy(isLoadingStats = false) }
            }
        }
    }
}
