package com.lambao.animike.ui.persondetail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.lambao.animike.domain.model.PersonDetail
import com.lambao.animike.domain.model.PersonStaffCredit
import com.lambao.animike.domain.model.PersonVoiceRole
import com.lambao.animike.ui.components.AnimeCard
import com.lambao.animike.ui.components.BackButton
import com.lambao.animike.ui.components.ExpandableText
import com.lambao.animike.ui.components.ScrollToTopButton
import com.lambao.animike.ui.theme.Dimens
import com.lambao.animike.ui.theme.Motion
import kotlinx.coroutines.launch

@Composable
fun PersonDetailScreen(
    onBackClick: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    viewModel: PersonDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is PersonDetailEffect.NavigateToDetail -> onNavigateToDetail(effect.malId)
            }
        }
    }

    PersonDetailScreenContent(state = state, onBackClick = onBackClick, onEvent = viewModel::onEvent)
}

@Composable
private fun PersonDetailScreenContent(
    state: PersonDetailState,
    onBackClick: () -> Unit,
    onEvent: (PersonDetailEvent) -> Unit,
) {
    // contentWindowInsets = 0: AniMikeNavHost đã có Scaffold ngoài tiêu thụ
    // insets — tránh tiêu thụ 2 lần gây khoảng trắng dư quanh status bar.
    Scaffold(modifier = Modifier.fillMaxSize(), contentWindowInsets = WindowInsets(0)) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                // imePadding: co lại theo bàn phím để bringIntoView (ô tìm
                // kiếm "Vai diễn lồng tiếng") tính đúng khoảng che khuất.
                .imePadding()
                .background(MaterialTheme.colorScheme.background),
        ) {
            when {
                state.person != null -> PersonDetailContent(
                    person = state.person,
                    staffCredits = state.staffCredits,
                    voiceRoles = state.voiceRoles,
                    groupedVoiceRoles = state.groupedVoiceRoles,
                    voiceSearchQuery = state.voiceSearchQuery,
                    onEvent = onEvent,
                )

                state.isLoading -> LoadingContent()

                state.error != null -> ErrorContent(
                    message = state.error,
                    onRetry = { onEvent(PersonDetailEvent.OnRetry) },
                )

                else -> LoadingContent()
            }

            BackButton(onClick = onBackClick, modifier = Modifier.padding(Dimens.SpaceSm))
        }
    }
}

private enum class PersonTab { STAFF_CREDITS, VOICE_ROLES }

// Ngưỡng hiện nút "cuộn lên đầu" — cùng giá trị SCROLL_TO_TOP_THRESHOLD của
// DetailScreen, áp dụng cho CẢ 2 tab vì dùng chung 1 listState/LazyColumn
// (yêu cầu user: "áp dụng cho tất cả các tab").
private const val SCROLL_TO_TOP_THRESHOLD = 6

// Chỉ hiện ô tìm kiếm khi danh sách đủ dài để cần tìm — dưới ngưỡng này thao
// tác cuộn tay đã đủ nhanh, ô tìm kiếm chỉ chiếm chỗ vô ích.
private const val VOICE_SEARCH_MIN_ITEMS = 15

// Số cột lưới "Vai trò sản xuất" — 3 cột khớp quy ước AnimeCard grid đã có
// (StudioDetailScreen's anime đã sản xuất), khác LazyRow ngang trước đây.
private const val STAFF_GRID_COLUMNS = 3

@Composable
private fun PersonDetailContent(
    person: PersonDetail,
    staffCredits: List<PersonStaffCredit>,
    voiceRoles: List<PersonVoiceRole>,
    groupedVoiceRoles: List<VoiceRoleGroup>,
    voiceSearchQuery: String,
    onEvent: (PersonDetailEvent) -> Unit,
) {
    // buildList: chỉ liệt kê tab THỰC SỰ có data — cùng pattern DetailTab ở
    // DetailScreen (ExploreTabsSection).
    val availableTabs = remember(staffCredits, voiceRoles) {
        buildList {
            if (staffCredits.isNotEmpty()) add(PersonTab.STAFF_CREDITS)
            if (voiceRoles.isNotEmpty()) add(PersonTab.VOICE_ROLES)
        }
    }
    var selectedTab by rememberSaveable { mutableStateOf(PersonTab.STAFF_CREDITS) }
    val effectiveTab = if (selectedTab in availableTabs) selectedTab else availableTabs.firstOrNull()

    // Hoist listState + cuộn về đầu khi đổi tab — nội dung 2 tab nằm CHUNG 1
    // LazyColumn (không phải 2 LazyColumn riêng như AnimatedContent sẽ tạo),
    // nên vị trí cuộn KHÔNG tự reset: đổi từ tab "Vai diễn lồng tiếng" (541
    // item, có thể đang cuộn sâu) sang "Vai trò sản xuất" (~15 item) sẽ bị
    // clamp/lạc vị trí nếu không chủ động cuộn lại (phát hiện qua review, sửa).
    // scrollToItem (không animate) — tránh hiệu ứng "nhảy xuống đáy list ngắn
    // rồi animate ngược lên 0" thoáng qua khi LazyColumn tự clamp vị trí cuộn
    // trong 1 frame trước khi hiệu ứng kịp chạy (góp ý từ review).
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val rows = remember(staffCredits) { staffCredits.chunked(STAFF_GRID_COLUMNS) }
    LaunchedEffect(effectiveTab) { listState.scrollToItem(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
            item { PersonHero(person = person) }

            // Stat strip vivid: 2 số liệu nhanh cho thấy ngay độ "chăm chỉ" của
            // người này — không có ở Character Detail (sáng tạo riêng cho People).
            item {
                StatStrip(voiceRoleCount = voiceRoles.size, staffCreditCount = staffCredits.size)
            }

            if (person.alternateNames.isNotEmpty()) {
                item { AlternateNameChips(names = person.alternateNames) }
            }

            if (!person.about.isNullOrBlank()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm)) {
                        Text(
                            text = "Tiểu sử",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        ExpandableText(
                            text = person.about,
                            modifier = Modifier.padding(top = Dimens.SpaceXs),
                            maxCollapsedLines = 4,
                        )
                    }
                }
            }

            if (availableTabs.size >= 2) {
                item {
                    PersonTabRow(
                        tabs = availableTabs,
                        selectedTab = effectiveTab,
                        onTabSelected = { selectedTab = it },
                    )
                }
            } else if (availableTabs.size == 1) {
                item {
                    SectionTitle(personTabLabel(availableTabs.first()))
                }
            }

            // KHÔNG dùng AnimatedContent crossfade (khác ExploreTabsSection ở
            // DetailScreen) — tab "Vai diễn lồng tiếng" có thể tới vài trăm item,
            // phải dùng items() TRỰC TIẾP trong LazyColumn ngoài cùng để tận dụng
            // lazy loading/recycle thật sự; AnimatedContent chỉ bọc được 1
            // composable "trọn gói" nên sẽ ép toàn bộ list compose 1 lần, mất hẳn
            // lợi ích lazy — đánh đổi lấy hiệu năng thay vì hiệu ứng chuyển tab.
            when (effectiveTab) {
                PersonTab.STAFF_CREDITS -> {
                    // Lưới dọc 3 cột (thay LazyRow ngang trước đây) — chunked
                    // thành từng hàng, mỗi hàng là 1 item của CHÍNH LazyColumn
                    // ngoài cùng (không lồng LazyVerticalGrid vào LazyColumn —
                    // vi phạm nested scrollable), vẫn lazy theo từng hàng.

                    items(rows, key = { row -> row.first().anime.malId }) { row ->
                        StaffCreditGridRow(
                            row = row,
                            onAnimeClick = { onEvent(PersonDetailEvent.OnAnimeClick(it)) },
                        )
                    }
                }

                PersonTab.VOICE_ROLES -> {
                    // Ô tìm kiếm chỉ hiện khi danh sách GỐC (chưa lọc) đủ dài
                    // — dùng voiceRoles.size (không phải filtered) để ô không
                    // biến mất giữa chừng khi user đang gõ làm filtered co lại.
                    if (voiceRoles.size >= VOICE_SEARCH_MIN_ITEMS) {
                        item {
                            VoiceSearchField(
                                query = voiceSearchQuery,
                                onQueryChange = { onEvent(PersonDetailEvent.OnVoiceSearchQueryChange(it)) },
                                modifier = Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm),
                            )
                        }
                    }
                    if (groupedVoiceRoles.isEmpty()) {
                        item {
                            Text(
                                text = "Không tìm thấy vai diễn",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceMd),
                            )
                        }
                    } else {
                        // Nhóm theo anime (1 người có thể lồng nhiều nhân vật
                        // trong cùng phim) — mỗi nhóm 1 item (header anime +
                        // các dòng nhân vật, đã sort Chính trước Phụ ở State).
                        items(groupedVoiceRoles, key = { it.anime.malId }) { group ->
                            VoiceRoleGroupItem(
                                group = group,
                                onClick = { onEvent(PersonDetailEvent.OnAnimeClick(group.anime.malId)) },
                            )
                        }
                    }
                }

                null -> Unit
            }
            item { Spacer(Modifier.height(Dimens.SpaceXl)) }
        }

        // Nút nổi "cuộn lên đầu" — áp dụng cho CẢ 2 tab (yêu cầu user), vì
        // dùng chung 1 listState bất kể tab nào đang hiện.
        val showScrollToTop by remember {
            derivedStateOf { listState.firstVisibleItemIndex > SCROLL_TO_TOP_THRESHOLD }
        }
        AnimatedVisibility(
            visible = showScrollToTop,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(Dimens.ScreenPadding),
            enter = fadeIn(animationSpec = tween(Motion.DurationShort4, easing = Motion.EasingEmphasizedDecelerate)),
            exit = fadeOut(animationSpec = tween(Motion.DurationShort4, easing = Motion.EasingEmphasizedAccelerate)),
        ) {
            ScrollToTopButton(onClick = { coroutineScope.launch { listState.animateScrollToItem(0) } })
        }
    }
}

// Hero portrait — cùng cấu trúc Character Detail nhưng đổi accent sang
// `primary` (tím) để phân biệt "người thật" khỏi "nhân vật hư cấu" (quyết
// định thiết kế đã thống nhất, xem docs/ROADMAP.md mục MVP5).
@Composable
private fun PersonHero(person: PersonDetail) {
    val placeholderColor = MaterialTheme.colorScheme.surfaceVariant
    val placeholderPainter = remember(placeholderColor) { ColorPainter(placeholderColor) }
    val background = MaterialTheme.colorScheme.background

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.HeroHeaderHeight),
    ) {
        AsyncImage(
            model = person.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            placeholder = placeholderPainter,
            error = placeholderPainter,
            fallback = placeholderPainter,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(colors = listOf(Color.Transparent, background.copy(alpha = 0.85f))),
                ),
        )
        if (person.favorites > 0) {
            FavoritesBadge(
                favorites = person.favorites,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(Dimens.ScreenPadding),
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
        ) {
            Text(
                text = person.name,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (person.birthday != null) {
                Text(
                    text = "🎂 ${person.birthday}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// Badge nền surface bán trong suốt đè trên ảnh (cùng style Character Detail)
// — accent primary (khác secondary của Character Detail) để khớp tông "người
// thật" của People Detail.
@Composable
private fun FavoritesBadge(favorites: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Dimens.RadiusChip))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
            .padding(horizontal = Dimens.SpaceSm, vertical = Dimens.SpaceXs),
    ) {
        Text(
            text = "♥ $favorites",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

// Sáng tạo riêng cho People Detail: 2 số liệu nhanh (số vai diễn/số tác
// phẩm) cho cảm giác "career snapshot" ngay dưới hero — Character Detail
// không có mục này. Ẩn từng chip nếu 0 (không hiện "0 vai diễn").
@Composable
private fun StatStrip(voiceRoleCount: Int, staffCreditCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        if (voiceRoleCount > 0) StatChip(icon = "🎙", label = "$voiceRoleCount vai diễn")
        if (staffCreditCount > 0) StatChip(icon = "🎬", label = "$staffCreditCount tác phẩm")
    }
}

@Composable
private fun StatChip(icon: String, label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(Dimens.RadiusChip))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            .padding(horizontal = Dimens.SpaceMd, vertical = Dimens.SpaceXs),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
    ) {
        Text(text = icon, style = MaterialTheme.typography.labelLarge)
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun AlternateNameChips(names: List<String>) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        items(names, key = { it }) { name ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Dimens.RadiusChip))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = Dimens.SpaceMd, vertical = Dimens.SpaceXs),
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
    )
}

private fun personTabLabel(tab: PersonTab): String = when (tab) {
    PersonTab.STAFF_CREDITS -> "Vai trò sản xuất"
    PersonTab.VOICE_ROLES -> "Vai diễn lồng tiếng"
}

// TabRow chuẩn Material3 (cùng lý do DetailTabRow ở DetailScreen — tự lo đo
// width chia đều + vẽ indicator trượt mượt). `tabs` là danh sách ĐỘNG (chỉ
// tab thực sự có data) nên dùng index trong list này, KHÔNG dùng ordinal.
@Composable
private fun PersonTabRow(tabs: List<PersonTab>, selectedTab: PersonTab?, onTabSelected: (PersonTab) -> Unit) {
    TabRow(
        selectedTabIndex = tabs.indexOf(selectedTab).coerceAtLeast(0),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
    ) {
        tabs.forEach { tab ->
            Tab(
                selected = tab == selectedTab,
                onClick = { onTabSelected(tab) },
                text = {
                    Text(text = personTabLabel(tab), style = MaterialTheme.typography.labelLarge)
                },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// 1 hàng của lưới dọc 3 cột — chunked() có thể trả hàng cuối THIẾU đủ
// STAFF_GRID_COLUMNS phần tử, thêm Spacer(weight) bù chỗ trống để card không
// bị giãn full-width sai tỉ lệ.
@Composable
private fun StaffCreditGridRow(row: List<PersonStaffCredit>, onAnimeClick: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceXs),
        horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap),
    ) {
        row.forEach { credit ->
            Column(modifier = Modifier.weight(1f)) {
                AnimeCard(
                    anime = credit.anime,
                    onClick = { onAnimeClick(credit.anime.malId) },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (credit.positions.isNotEmpty()) {
                    Text(
                        text = credit.positions.joinToString(", "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = Dimens.SpaceXs),
                    )
                }
            }
        }
        repeat(STAFF_GRID_COLUMNS - row.size) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun VoiceSearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    // BringIntoViewRequester: khi ô này nhận focus, tự cuộn LazyColumn cha để
    // đẩy ô lên trên, tránh bàn phím che mất danh sách bên dưới (yêu cầu
    // user) — kết hợp .imePadding() ở Box ngoài cùng để tính đúng khoảng che.
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(text = "Tìm theo nhân vật/anime...", style = MaterialTheme.typography.bodyMedium)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                Text(
                    text = "✕",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clickable { onQueryChange("") }
                        .semantics { contentDescription = "Xóa tìm kiếm" }
                        .padding(Dimens.SpaceMd),
                )
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(Dimens.RadiusButton),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusEvent { focusState ->
                if (focusState.isFocused) {
                    coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
                }
            },
    )
}

// Nhóm các vai diễn TRÙNG anime (yêu cầu user) — header hiện poster + tên
// anime 1 LẦN, các dòng nhân vật bên dưới đã sort Chính->Phụ ở State
// (PersonDetailState.groupedVoiceRoles), KHÔNG lặp lại ảnh/tên anime cho mỗi
// nhân vật như VoiceRoleItem (duo thumbnail) trước đây.
@Composable
private fun VoiceRoleGroupItem(group: VoiceRoleGroup, onClick: () -> Unit) {
    val placeholderColor = MaterialTheme.colorScheme.surfaceVariant
    val placeholderPainter = remember(placeholderColor) { ColorPainter(placeholderColor) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceXs),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
        ) {
            AsyncImage(
                model = group.anime.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                placeholder = placeholderPainter,
                error = placeholderPainter,
                fallback = placeholderPainter,
                modifier = Modifier
                    .size(width = Dimens.VoiceRoleThumbnailWidth, height = Dimens.VoiceRoleThumbnailHeight)
                    .clip(RoundedCornerShape(Dimens.RadiusChip)),
            )
            Text(
                text = group.anime.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        group.roles.forEach { role ->
            VoiceRoleCharacterRow(role = role)
        }
    }
}

// Thụt lề theo đúng chiều rộng poster + spacing ở header phía trên, để avatar
// nhân vật thẳng hàng dưới poster anime thay vì dính sát lề trái.
@Composable
private fun VoiceRoleCharacterRow(role: PersonVoiceRole) {
    val placeholderColor = MaterialTheme.colorScheme.surfaceVariant
    val placeholderPainter = remember(placeholderColor) { ColorPainter(placeholderColor) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Dimens.VoiceRoleThumbnailWidth + Dimens.SpaceSm, top = Dimens.SpaceXs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        AsyncImage(
            model = role.characterImageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            placeholder = placeholderPainter,
            error = placeholderPainter,
            fallback = placeholderPainter,
            modifier = Modifier
                .size(Dimens.VoiceRoleCharacterBadgeSize)
                .clip(CircleShape),
        )
        Text(
            text = role.characterName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (role.role.isNotEmpty()) VoiceRoleBadge(role = role.role)
    }
}

@Composable
private fun VoiceRoleBadge(role: String) {
    val isMain = role.equals("Main", ignoreCase = true)
    val label = if (isMain) "Chính" else "Phụ"
    val color = if (isMain) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Dimens.RadiusChip))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = Dimens.SpaceSm, vertical = Dimens.SpaceXs),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize()) {
        CircularProgressIndicator(
            modifier = Modifier.align(Alignment.Center),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = Dimens.ScreenPadding),
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
