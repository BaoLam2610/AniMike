package com.lambao.animike.ui.reviews

// PagingData không đặt trong state (compose-expert/references/paging-mvi-
// testing.md) — expose riêng `items` bên cạnh, cùng pattern với EpisodesScreen
// (không có filter/selection, malId cố định, retry lỗi phân trang gọi thẳng
// pagingItems.retry() từ Compose) — giữ empty cho khớp
// BaseViewModel<State, Event, Effect>.
data object ReviewsState

sealed interface ReviewsEvent

sealed interface ReviewsEffect
