package com.lambao.animike.ui.reviews

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.lambao.animike.data.repository.toAppError
import com.lambao.animike.domain.model.AnimeReview
import com.lambao.animike.domain.model.AnimeStatistics
import com.lambao.animike.domain.model.ScoreDistributionEntry
import com.lambao.animike.domain.model.toUserMessage
import com.lambao.animike.ui.components.BackButton
import com.lambao.animike.ui.components.ReviewCard
import com.lambao.animike.ui.components.rememberShimmerProgress
import com.lambao.animike.ui.components.shimmerEffect
import com.lambao.animike.ui.theme.Dimens
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ReviewsScreen(
    onBackClick: () -> Unit,
    onNavigateToReviewDetail: () -> Unit,
    viewModel: ReviewsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pagingItems = viewModel.items.collectAsLazyPagingItems()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ReviewsEffect.NavigateToReviewDetail -> onNavigateToReviewDetail()
            }
        }
    }

    ReviewsScreenContent(
        state = state,
        pagingItems = pagingItems,
        onBackClick = onBackClick,
        onEvent = viewModel::onEvent,
    )
}

@Composable
private fun ReviewsScreenContent(
    state: ReviewsState,
    pagingItems: LazyPagingItems<AnimeReview>,
    onBackClick: () -> Unit,
    onEvent: (ReviewsEvent) -> Unit,
) {
    // contentWindowInsets = 0: AniMikeNavHost đã có Scaffold ngoài tiêu thụ
    // insets — tránh tiêu thụ 2 lần gây khoảng trắng dư quanh status bar.
    Scaffold(modifier = Modifier.fillMaxSize(), contentWindowInsets = WindowInsets(0)) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.SpaceSm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            ) {
                BackButton(onClick = onBackClick)
                Text(
                    text = "Đánh giá",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            when {
                // Không check itemCount == 0: giống Episodes/Schedules — ưu
                // tiên refresh-loading, dù ở đây malId cố định không có
                // filter/selection để tạo lại Pager giữa chừng.
                // Statistics là dữ liệu ĐỘC LẬP với Paging (Room SWR riêng) —
                // vẫn hiện ở cả 3 nhánh Loading/Error/Empty này (pinned, giống
                // hành vi trước khi dời vào LazyColumn) để không mất số liệu
                // hợp lệ chỉ vì review list đang tải/lỗi/rỗng (phát hiện qua
                // review: nếu chỉ hiện trong nhánh "list", statistics sẽ biến
                // mất hẳn khi anime có thống kê nhưng CHƯA có review nào).
                pagingItems.loadState.refresh is LoadState.Loading -> Column(Modifier.fillMaxSize()) {
                    StatisticsSection(statistics = state.statistics)
                    ReviewsListLoading(modifier = Modifier.weight(1f))
                }

                pagingItems.loadState.refresh is LoadState.Error && pagingItems.itemCount == 0 -> {
                    val error = (pagingItems.loadState.refresh as LoadState.Error).error
                    Column(Modifier.fillMaxSize()) {
                        StatisticsSection(statistics = state.statistics)
                        ReviewsErrorContent(
                            message = error.toAppError().toUserMessage(),
                            onRetry = { pagingItems.retry() },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                pagingItems.itemCount == 0 -> Column(Modifier.fillMaxSize()) {
                    StatisticsSection(statistics = state.statistics)
                    ReviewsEmptyContent(modifier = Modifier.weight(1f))
                }

                // "Thống kê" đặt làm item ĐẦU trong chính LazyColumn danh
                // sách review (không còn pinned) — theo yêu cầu user: cuộn
                // xuống thì thống kê cuộn theo luôn, không chiếm chỗ cố định
                // trên đầu màn hình (chỉ áp dụng khi ĐÃ có review để cuộn).
                else -> ReviewsPagingList(
                    statistics = state.statistics,
                    pagingItems = pagingItems,
                    onReviewClick = { onEvent(ReviewsEvent.OnReviewClick(it)) },
                )
            }
        }
    }
}

// MVP4 "Biểu đồ phân bố điểm + số người xem" (/anime/{id}/statistics) —
// CHUYỂN từ Detail sang đây (theo yêu cầu user: Detail đã quá nhiều section,
// thống kê hợp lý hơn khi đặt cạnh danh sách Đánh giá đầy đủ, giống pattern
// quen thuộc của Play Store/App Store). Không có mockup riêng, tự thiết kế
// thanh ngang theo token animike-design (lấy cảm hứng bố cục từ docs/UI "give
// rating" nhưng đó là input rating cá nhân — ở đây CHỈ hiển thị số liệu MAL,
// không cho user chấm điểm, việc đó để dành MVP6). statistics tải async nên
// null lúc đầu — AnimatedVisibility để section "mọc" mượt khi tải xong. Đặt
// làm item ĐẦU trong LazyColumn của ReviewsPagingList (không phải sibling cố
// định phía trên) — theo yêu cầu user: cuộn xuống thì thống kê cuộn theo
// cùng danh sách review, không chiếm chỗ cố định trên đầu màn hình.
@Composable
private fun StatisticsSection(statistics: AnimeStatistics?) {
    AnimatedVisibility(visible = statistics != null) {
        Column(
            modifier = Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
        ) {
            Text(
                text = "Thống kê",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
                // sortedByDescending: kit tham khảo ("give rating") hiện điểm cao
                // ở trên — Jikan trả scores không đảm bảo thứ tự cố định.
                statistics?.scoreDistribution.orEmpty().sortedByDescending { it.score }.forEach { entry ->
                    key(entry.score) { ScoreDistributionRow(entry = entry) }
                }
            }
            if (statistics != null) {
                StatisticsSummaryRow(statistics = statistics)
            }
        }
    }
}

@Composable
private fun ScoreDistributionRow(entry: ScoreDistributionEntry) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        Text(
            text = "${entry.score}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(Dimens.ScoreBarLabelWidth),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(Dimens.ScoreBarHeight)
                .clip(RoundedCornerShape(Dimens.ScoreBarHeight / 2))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = (entry.percentage / 100.0).toFloat().coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(Dimens.ScoreBarHeight / 2))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
        Text(
            // Locale.US ép cố định dấu chấm thập phân (không phụ thuộc locale
            // máy — vi-VN dùng dấu phẩy cho thập phân, sẽ lệch khỏi format "34.6%"
            // quen thuộc của mọi app điểm số khác).
            text = String.format(Locale.US, "%.1f%%", entry.percentage),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(Dimens.ScoreBarPercentWidth),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun StatisticsSummaryRow(statistics: AnimeStatistics) {
    // FlowRow chưa import sẵn trong file này (chỉ dùng Row/Column) — 6 mục
    // ngắn nên đủ chỗ trên 1 hàng cuộn ngang thay vì thêm dependency layout mới.
    LazyRow(
        contentPadding = PaddingValues(vertical = Dimens.SpaceSm),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceLg),
    ) {
        item { StatisticsSummaryItem(label = "Đang xem", value = statistics.watching) }
        item { StatisticsSummaryItem(label = "Đã xem", value = statistics.completed) }
        item { StatisticsSummaryItem(label = "Tạm dừng", value = statistics.onHold) }
        item { StatisticsSummaryItem(label = "Đã bỏ", value = statistics.dropped) }
        item { StatisticsSummaryItem(label = "Dự định xem", value = statistics.planToWatch) }
        item { StatisticsSummaryItem(label = "Tổng", value = statistics.total) }
    }
}

@Composable
private fun StatisticsSummaryItem(label: String, value: Int) {
    Column {
        Text(
            text = formatMemberCount(value),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Locale vi-VN dùng dấu chấm phân cách hàng nghìn (khác dấu phẩy en-US) —
// khớp ngôn ngữ hiển thị còn lại của app; số người xem MAL thường tới hàng
// triệu nên cần phân cách để dễ đọc.
private fun formatMemberCount(value: Int): String =
    NumberFormat.getNumberInstance(Locale("vi", "VN")).format(value)

@Composable
private fun ReviewsPagingList(
    statistics: AnimeStatistics?,
    pagingItems: LazyPagingItems<AnimeReview>,
    onReviewClick: (AnimeReview) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
    ) {
        item { StatisticsSection(statistics = statistics) }

        // items(count, key, contentType, itemContent) là member function của
        // LazyListScope — không cần import (giống pattern ScheduleList).
        items(
            count = pagingItems.itemCount,
            key = pagingItems.itemKey { it.id },
            contentType = pagingItems.itemContentType { "review" },
        ) { index ->
            pagingItems[index]?.let { review -> ReviewCard(review = review, onClick = { onReviewClick(review) }) }
        }

        if (pagingItems.loadState.append is LoadState.Loading) {
            item {
                val shimmerProgress = rememberShimmerProgress()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.ReviewCardPlaceholderHeight)
                        .clip(RoundedCornerShape(Dimens.RadiusCard))
                        .shimmerEffect(shimmerProgress),
                )
            }
        }

        if (pagingItems.loadState.append is LoadState.Error) {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TextButton(onClick = { pagingItems.retry() }) {
                        Text("Tải thêm thất bại — Thử lại")
                    }
                }
            }
        }
    }
}

// modifier mặc định fillMaxSize() (đứng 1 mình) — call site truyền
// Modifier.weight(1f) khi ghép chung Column với StatisticsSection phía trên.
@Composable
private fun ReviewsListLoading(modifier: Modifier = Modifier.fillMaxSize()) {
    val shimmerProgress = rememberShimmerProgress()
    Column(
        modifier = modifier.padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
    ) {
        repeat(4) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.ReviewCardPlaceholderHeight)
                    .clip(RoundedCornerShape(Dimens.RadiusCard))
                    .shimmerEffect(shimmerProgress),
            )
        }
    }
}

@Composable
private fun ReviewsErrorContent(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier.fillMaxSize()) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
        ) {
            Text(text = "(￣ヘ￣)", style = MaterialTheme.typography.displaySmall)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onRetry) {
                Text("Thử lại")
            }
        }
    }
}

@Composable
private fun ReviewsEmptyContent(modifier: Modifier = Modifier.fillMaxSize()) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = "Không có đánh giá nào",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
