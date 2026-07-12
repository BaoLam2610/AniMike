package com.lambao.animike.ui.debug

import androidx.compose.runtime.Immutable
import com.lambao.animike.data.repository.TableStat
import com.lambao.animike.debug.AppLogEntry
import com.lambao.animike.debug.NetworkLogEntry

// MVP-Debug Đợt 1 — màn Debug (mở từ FAB nổi, chỉ DEBUG build). 3 tab: gọi
// API / log local / cache. State/Event/Effect theo MVI như mọi màn khác.
enum class DebugTab { NETWORK, LOG, CACHE }

@Immutable
data class DebugState(
    val selectedTab: DebugTab = DebugTab.NETWORK,
    // networkLogs/appLogs stream thẳng từ DebugInspector (ring-buffer RAM),
    // ViewModel chỉ mirror vào state; tableStats load theo yêu cầu (one-shot).
    val networkLogs: List<NetworkLogEntry> = emptyList(),
    val appLogs: List<AppLogEntry> = emptyList(),
    val tableStats: List<TableStat> = emptyList(),
    val isLoadingStats: Boolean = false,
    // Từ khoá search riêng từng tab (giữ khi đổi tab qua lại) — lọc ở UI qua
    // remember(list, query) cho nhẹ, không đưa list đã lọc vào state.
    val networkQuery: String = "",
    val logQuery: String = "",
    val cacheQuery: String = "",
)

sealed interface DebugEvent {
    data class OnTabSelected(val tab: DebugTab) : DebugEvent
    data object OnClearNetworkLogs : DebugEvent
    data object OnClearAppLogs : DebugEvent
    // Load lại số row các bảng (mở tab Cache / sau khi xoá).
    data object OnRefreshStats : DebugEvent
    // Xoá toàn bộ bảng cache (giữ favorite/tracking).
    data object OnClearCache : DebugEvent
    // Xoá 1 bảng cụ thể (kể cả user-data, UI đã confirm trước khi gửi).
    data class OnClearTable(val name: String) : DebugEvent
    data class OnNetworkQueryChange(val query: String) : DebugEvent
    data class OnLogQueryChange(val query: String) : DebugEvent
    data class OnCacheQueryChange(val query: String) : DebugEvent
    // Bấm 1 request → mở màn chi tiết (Đợt 2). id = NetworkLogEntry.id.
    data class OnNetworkLogClick(val id: Long) : DebugEvent
    // Bấm 1 bảng → mở màn xem schema + dòng mẫu (Đợt 2).
    data class OnTableClick(val name: String) : DebugEvent
}

sealed interface DebugEffect {
    data class ShowMessage(val text: String) : DebugEffect
    data class NavigateToNetworkDetail(val id: Long) : DebugEffect
    data class NavigateToTableDetail(val name: String) : DebugEffect
}
