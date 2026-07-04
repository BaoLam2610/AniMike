package com.lambao.animike.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lambao.animike.domain.model.Genre
import com.lambao.animike.domain.model.SearchFilters
import com.lambao.animike.ui.components.BackButton
import com.lambao.animike.ui.theme.Dimens
import java.util.Calendar

private val YEARS = run {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    (currentYear downTo currentYear - 11).toList()
}

@Composable
fun SearchFilterScreen(
    onBackClick: () -> Unit,
    // KHÔNG có default hiltViewModel(): màn này bắt buộc dùng CÙNG instance
    // SearchViewModel với SearchScreen (AniMikeNavHost scope theo backstack
    // entry của Routes.SEARCH). Nếu để default, gọi thiếu override sẽ âm thầm
    // tạo instance riêng scope theo route filter — Apply ghi vào ViewModel
    // "chết" không ai đọc, filter lặng lẽ không có tác dụng.
    viewModel: SearchViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Draft cục bộ (kit Animax MVP3 UI-6, nút Reset/Apply) — sửa filter ở màn
    // này KHÔNG áp ngay, chỉ commit vào SearchViewModel khi bấm Apply. Khác
    // hẳn FilterRow cũ (mỗi tap gọi onEvent ngay lập tức, tạo Pager mới liền).
    var draft by remember { mutableStateOf(state.filters) }

    SearchFilterScreenContent(
        draft = draft,
        genres = state.genres,
        onDraftChange = { draft = it },
        onReset = { draft = SearchFilters() },
        onApply = {
            viewModel.onEvent(SearchEvent.OnFiltersApplied(draft))
            onBackClick()
        },
        onBackClick = onBackClick,
    )
}

@Composable
private fun SearchFilterScreenContent(
    draft: SearchFilters,
    genres: List<Genre>,
    onDraftChange: (SearchFilters) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onBackClick: () -> Unit,
) {
    // contentWindowInsets = 0: AniMikeNavHost đã có Scaffold ngoài tiêu thụ
    // insets — tránh tiêu thụ 2 lần gây khoảng trắng dư.
    Scaffold(modifier = Modifier.fillMaxSize(), contentWindowInsets = WindowInsets(0)) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            ) {
                BackButton(onClick = onBackClick)
                Text(
                    text = "Sắp xếp & Lọc",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceMd),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceLg),
            ) {
                item {
                    SortSection(
                        orderBy = draft.orderBy,
                        sort = draft.sort,
                        onChange = { orderBy, sort -> onDraftChange(draft.copy(orderBy = orderBy, sort = sort)) },
                    )
                }
                item {
                    CategorySection(
                        type = draft.type,
                        onChange = { type -> onDraftChange(draft.copy(type = type)) },
                    )
                }
                // Region (All/Japan/Chinese/Others) trong kit bị bỏ qua — Jikan
                // /anime search không có tham số lọc theo quốc gia sản xuất.
                if (genres.isNotEmpty()) {
                    item {
                        GenreSection(
                            genres = genres,
                            selectedIds = draft.genreIds,
                            onToggle = { genreId ->
                                val updated = if (genreId in draft.genreIds) {
                                    draft.genreIds - genreId
                                } else {
                                    draft.genreIds + genreId
                                }
                                onDraftChange(draft.copy(genreIds = updated))
                            },
                        )
                    }
                }
                item {
                    YearSection(
                        selectedYear = draft.year,
                        onChange = { year -> onDraftChange(draft.copy(year = year)) },
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.ScreenPadding),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
            ) {
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(Dimens.RadiusButton),
                ) {
                    Text("Đặt lại")
                }
                Button(
                    onClick = onApply,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(Dimens.RadiusButton),
                ) {
                    Text("Áp dụng")
                }
            }
        }
    }
}

@Composable
private fun SortSection(orderBy: String, sort: String, onChange: (orderBy: String, sort: String) -> Unit) {
    FilterSection(title = "Sắp xếp") {
        SelectableChip(label = "Phổ biến", selected = orderBy == "popularity") {
            onChange("popularity", "asc")
        }
        SelectableChip(label = "Điểm cao", selected = orderBy == "score") {
            onChange("score", "desc")
        }
        SelectableChip(label = "Mới nhất", selected = orderBy == "start_date") {
            onChange("start_date", "desc")
        }
    }
}

@Composable
private fun CategorySection(type: String?, onChange: (String?) -> Unit) {
    FilterSection(title = "Danh mục") {
        SelectableChip(label = "Tất cả", selected = type == null) { onChange(null) }
        SelectableChip(label = "TV", selected = type == "tv") { onChange("tv") }
        SelectableChip(label = "Movie", selected = type == "movie") { onChange("movie") }
    }
}

@Composable
private fun GenreSection(genres: List<Genre>, selectedIds: Set<Int>, onToggle: (Int) -> Unit) {
    FilterSection(title = "Thể loại") {
        genres.forEach { genre ->
            SelectableChip(label = genre.name, selected = genre.id in selectedIds) { onToggle(genre.id) }
        }
    }
}

@Composable
private fun YearSection(selectedYear: Int?, onChange: (Int?) -> Unit) {
    FilterSection(title = "Năm phát hành") {
        SelectableChip(label = "Tất cả", selected = selectedYear == null) { onChange(null) }
        YEARS.forEach { year ->
            SelectableChip(label = "$year", selected = selectedYear == year) { onChange(year) }
        }
    }
}

@Composable
private fun FilterSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
        ) {
            content()
        }
    }
}

// internal (không private): dùng chung trong package ui.search — cả các section
// ở màn filter này lẫn hàng tóm tắt filter đang áp dụng ở SearchScreen. Chưa
// tách ra ui/components vì mới 2 file dùng (quy ước: tách khi có từ 3 nơi).
@Composable
internal fun SelectableChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Dimens.RadiusChip))
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.SpaceMd, vertical = Dimens.SpaceSm),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
