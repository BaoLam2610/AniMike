package com.lambao.animike.ui.episodes

// PagingData không đặt trong state (compose-expert/references/paging-mvi-
// testing.md) — expose riêng `items` bên cạnh, cùng pattern với Search/
// Schedules/SeasonArchive. Màn này không có filter/selection nào khác ngoài
// malId cố định, và retry lỗi phân trang gọi thẳng pagingItems.retry() từ
// Compose — nên không cần State/Event/Effect nào, giữ empty cho khớp
// BaseViewModel<State, Event, Effect>.
data object EpisodesState

sealed interface EpisodesEvent

sealed interface EpisodesEffect
