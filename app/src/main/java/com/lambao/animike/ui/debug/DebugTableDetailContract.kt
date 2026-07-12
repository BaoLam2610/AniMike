package com.lambao.animike.ui.debug

import androidx.compose.runtime.Immutable
import com.lambao.animike.data.repository.TableDetail

// MVP-Debug Đợt 2 — màn xem schema + dòng mẫu của 1 bảng cache (mở từ tab
// Cache). Load one-shot qua DebugRepository.tableDetail.
@Immutable
data class DebugTableDetailState(
    val tableName: String = "",
    val detail: TableDetail? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    // Đợt 3 — số dòng đang chọn xem, null = "Tất cả" (bỏ SQL LIMIT).
    val rowLimit: Int? = DEFAULT_ROW_LIMIT,
)

// 20 mặc định (giữ nguyên hành vi Đợt 2), thêm 100/500/"Tất cả" cho user tự
// chọn thay vì phải sửa hằng số trong code (góp ý user).
val ROW_LIMIT_OPTIONS: List<Int?> = listOf(20, 100, 500, null)
const val DEFAULT_ROW_LIMIT = 20

sealed interface DebugTableDetailEvent {
    data object OnRetry : DebugTableDetailEvent
    data class OnRowLimitChanged(val limit: Int?) : DebugTableDetailEvent
}

sealed interface DebugTableDetailEffect
