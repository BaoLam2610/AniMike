package com.lambao.animike.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush

/**
 * Progress dùng chung cho mọi shimmer placeholder trong cùng 1 section —
 * hoist 1 lần ở call site thay vì mỗi placeholder tự tạo `InfiniteTransition`
 * riêng (VD LazyRow 6 placeholder chỉ chạy 1 animation, không phải 6).
 */
@Composable
fun rememberShimmerProgress(): Float {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = -1000f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_translate",
    )
    return progress
}

/** Shimmer placeholder nền `surfaceVariant` (animike-design SKILL.md). */
fun Modifier.shimmerEffect(progress: Float): Modifier = composed {
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    // drawWithCache + onDrawBehind: đọc `progress` ở draw phase, không phải
    // composition phase — tránh recomposition toàn modifier chain mỗi frame
    // (performance.md: "Defer State Reads to Layout/Draw Phase").
    Modifier.drawWithCache {
        val brush = Brush.linearGradient(
            colors = listOf(
                surfaceVariant.copy(alpha = 0.6f),
                surfaceVariant.copy(alpha = 0.2f),
                surfaceVariant.copy(alpha = 0.6f),
            ),
            start = Offset(progress, 0f),
            end = Offset(progress + 500f, 0f),
        )
        onDrawBehind { drawRect(brush) }
    }
}
