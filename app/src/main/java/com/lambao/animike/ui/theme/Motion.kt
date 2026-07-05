package com.lambao.animike.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

// Giá trị số lấy từ Material Design motion spec (compose-expert/references/
// material3-motion.md) — KHÔNG dùng được androidx.compose.material3.tokens.
// MotionTokens trực tiếp vì object đó `internal` trong module material3,
// không cross-module accessible từ app (build lỗi "Cannot access... internal
// in file"). Tự định nghĩa lại đúng con số/đường cong thay vì hardcode rải
// rác từng chỗ dùng animation trong app.
object Motion {
    const val DurationShort4 = 200 // chip selection, tooltip — dùng cho crossfade tab
    const val DurationMedium2 = 300 // dialog, bottom sheet, nav drawer — dùng cho slide/push

    // Enter/exit rule: element đến dùng Decelerate (vào nhanh, dừng êm),
    // element rời dùng Accelerate (ra chậm rồi tăng tốc) — không bao giờ
    // dùng chung 1 easing cho cả 2 chiều.
    val EasingEmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EasingEmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
}
