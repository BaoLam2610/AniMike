package com.lambao.animike.ui.debug

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.lambao.animike.debug.DebugInspector
import com.lambao.animike.ui.base.BaseViewModel
import com.lambao.animike.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class DebugNetworkDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<DebugNetworkDetailState, DebugNetworkDetailEvent, DebugNetworkDetailEffect>(
    DebugNetworkDetailState(),
) {

    private val id: Long = checkNotNull(savedStateHandle[Routes.DEBUG_NETWORK_DETAIL_ARG_ID])

    init {
        // Collect ring-buffer để nếu bị "Xoá log" khi đang xem thì màn tự cập
        // nhật sang trạng thái "không tìm thấy" thay vì hiện dữ liệu ma.
        DebugInspector.networkLogs
            .onEach { logs -> setState { copy(entry = logs.firstOrNull { it.id == id }, resolved = true) } }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: DebugNetworkDetailEvent) = Unit
}
