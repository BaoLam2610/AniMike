package com.lambao.animike.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.lambao.animike.domain.model.WatchStatus
import com.lambao.animike.ui.theme.success
import com.lambao.animike.ui.theme.warning

// MVP6 Tracking — bảng quy đổi WatchStatus -> nhãn/emoji/màu, dùng chung cho
// WatchStatusButton (nút + DropdownMenu trên TopBar của Detail) + filter
// chips/badge (màn "Danh sách"). Mỗi trạng thái 1 màu ngữ nghĩa riêng (như
// ReviewTagBadge 3 màu): Đang xem = success (đang "sống"), Đã xem = tertiary
// (hoàn tất, thông tin), Tạm dừng = warning/rankGold (hổ phách — cảnh báo
// nhẹ, tái dùng token medal), Bỏ = error, Dự định xem = primary (hướng tới
// tương lai — accent chính).

val WatchStatus.label: String
    get() = when (this) {
        WatchStatus.WATCHING -> "Đang xem"
        WatchStatus.COMPLETED -> "Đã xem"
        WatchStatus.ON_HOLD -> "Tạm dừng"
        WatchStatus.DROPPED -> "Bỏ"
        WatchStatus.PLAN_TO_WATCH -> "Dự định xem"
    }

// Lưu ý (từ review): ▶ ✓ ⏸ ✕ là ký hiệu đơn sắc nên Text(color=...) tô màu
// được bình thường; riêng 🕒 là pictograph nhiều màu (như 📅 ở StudioDetail),
// hệ điều hành sẽ tự vẽ màu gốc bất kể color truyền vào — chấp nhận được vì
// nhãn chữ cạnh nó vẫn lên đúng màu ngữ nghĩa, chỉ glyph không đổi màu (đã có
// tiền lệ tương tự với 📅 trong app). 🔖 (icon mặc định "chưa theo dõi" ở
// WatchStatusButton) cũng cùng tình trạng.
val WatchStatus.emoji: String
    get() = when (this) {
        WatchStatus.WATCHING -> "▶"
        WatchStatus.COMPLETED -> "✓"
        WatchStatus.ON_HOLD -> "⏸"
        WatchStatus.DROPPED -> "✕"
        WatchStatus.PLAN_TO_WATCH -> "🕒"
    }

@Composable
fun WatchStatus.statusColor(): Color = when (this) {
    WatchStatus.WATCHING -> MaterialTheme.colorScheme.success
    WatchStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary
    // warning (không phải rankGold trực tiếp) — alias semantic, xem Theme.kt.
    WatchStatus.ON_HOLD -> MaterialTheme.colorScheme.warning
    WatchStatus.DROPPED -> MaterialTheme.colorScheme.error
    WatchStatus.PLAN_TO_WATCH -> MaterialTheme.colorScheme.primary
}
