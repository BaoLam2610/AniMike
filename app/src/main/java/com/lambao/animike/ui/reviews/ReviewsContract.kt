package com.lambao.animike.ui.reviews

import androidx.compose.runtime.Immutable
import com.lambao.animike.domain.model.AnimeReview
import com.lambao.animike.domain.model.AnimeStatistics

// PagingData không đặt trong state (compose-expert/references/paging-mvi-
// testing.md) — expose riêng `items` bên cạnh, cùng pattern với EpisodesScreen
// (không có filter/selection, malId cố định, retry lỗi phân trang gọi thẳng
// pagingItems.retry() từ Compose).
// statistics: MVP4 "Biểu đồ phân bố điểm + số người xem" — CHUYỂN từ Detail
// sang đây (theo yêu cầu user: Detail đã quá nhiều section, thống kê hợp lý
// hơn khi đặt cạnh danh sách Đánh giá đầy đủ). null lúc đầu hoặc tải chưa
// xong — không dùng emptyList() vì là single object/anime, không phải list.
// selectedReview: review đang mở ở màn chi tiết (ReviewDetailScreen) — lưu
// trực tiếp OBJECT thay vì chỉ id, vì Paging 3 KHÔNG có cách tra cứu ngược
// "item theo id" (không phải Room, không thể observe lại) — ReviewDetailScreen
// dùng chung ViewModel này (hiltViewModel(parentBackStackEntry), giống
// SearchFilterScreen/SearchViewModel) nên đọc thẳng field này, không cần gọi
// lại API hay truyền qua nav argument.
@Immutable
data class ReviewsState(
    val statistics: AnimeStatistics? = null,
    val selectedReview: AnimeReview? = null,
)

sealed interface ReviewsEvent {
    data class OnReviewClick(val review: AnimeReview) : ReviewsEvent
}

sealed interface ReviewsEffect {
    data object NavigateToReviewDetail : ReviewsEffect
}
