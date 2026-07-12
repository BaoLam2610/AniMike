package com.lambao.animike.ui.detail

import android.Manifest
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.lambao.animike.domain.model.Anime
import com.lambao.animike.domain.model.AnimeCharacter
import com.lambao.animike.domain.model.AnimeDetail
import com.lambao.animike.domain.model.AnimeReview
import com.lambao.animike.domain.model.AnimeStaffMember
import com.lambao.animike.domain.model.AnimeThemes
import com.lambao.animike.domain.model.AnimeVideo
import com.lambao.animike.domain.model.Episode
import com.lambao.animike.domain.model.Picture
import com.lambao.animike.domain.model.RelationGroup
import com.lambao.animike.domain.model.StreamingLink
import com.lambao.animike.domain.model.Studio
import com.lambao.animike.domain.model.WatchStatus
import com.lambao.animike.ui.components.AnimeCard
import com.lambao.animike.ui.components.BackButton
import com.lambao.animike.ui.components.ExpandableText
import com.lambao.animike.ui.components.ReviewCard
import com.lambao.animike.ui.components.ScrollToTopButton
import com.lambao.animike.ui.components.emoji
import com.lambao.animike.ui.components.label
import com.lambao.animike.ui.components.statusColor
import com.lambao.animike.ui.theme.Dimens
import com.lambao.animike.ui.theme.Motion
import com.lambao.animike.ui.theme.success
import androidx.core.net.toUri
import kotlinx.coroutines.launch

@Composable
fun DetailScreen(
    onBackClick: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToEpisodes: (Int) -> Unit,
    onNavigateToCharacters: (Int) -> Unit,
    onNavigateToCharacterDetail: (Int) -> Unit,
    onNavigateToPersonDetail: (Int) -> Unit,
    onNavigateToStudioDetail: (Int) -> Unit,
    onNavigateToReviews: (Int) -> Unit,
    onNavigateToReviewDetail: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // WRITE_EXTERNAL_STORAGE là dangerous permission từ API 23 — CHỈ khai
    // trong Manifest KHÔNG đủ, phải xin runtime. Miễn trừ scoped-storage
    // (API 29+) áp dụng theo API CỦA THIẾT BỊ đang chạy (không phải targetSdk
    // của app — app này targetSdk 36 nhưng thiết bị thật chạy API 24-28 vẫn
    // enforce model cũ) — nên chỉ xin quyền khi SDK_INT nằm trong khoảng
    // 23..28, còn lại (29+) gọi thẳng downloadPicture(). Không có bước xin
    // quyền này, nút tải sẽ LUÔN thất bại (không phải hiếm) trên mọi thiết bị
    // API 24-28 thật — phát hiện qua review, sửa ngay.
    var pendingDownloadUrl by remember { mutableStateOf<String?>(null) }
    val requestStoragePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val url = pendingDownloadUrl
        pendingDownloadUrl = null
        if (granted && url != null) {
            downloadPicture(context, url)
        } else if (!granted) {
            Toast.makeText(context, "Cần quyền lưu trữ để tải ảnh xuống", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is DetailEffect.OpenYoutube -> {
                    val uri = "https://www.youtube.com/watch?v=${effect.videoId}".toUri()
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    } catch (e: ActivityNotFoundException) {
                        Log.w("DetailScreen", "Không có app nào xử lý được link YouTube", e)
                    }
                }

                is DetailEffect.OpenExternalUrl -> {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, effect.url.toUri()))
                    } catch (e: ActivityNotFoundException) {
                        Log.w("DetailScreen", "Không có app nào xử lý được link streaming", e)
                    }
                }

                is DetailEffect.DownloadPicture -> {
                    val needsRuntimePermission = Build.VERSION.SDK_INT in Build.VERSION_CODES.M until Build.VERSION_CODES.Q &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                        PackageManager.PERMISSION_GRANTED
                    if (needsRuntimePermission) {
                        pendingDownloadUrl = effect.url
                        requestStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    } else {
                        downloadPicture(context, effect.url)
                    }
                }

                is DetailEffect.NavigateToDetail -> onNavigateToDetail(effect.malId)
                is DetailEffect.NavigateToEpisodes -> onNavigateToEpisodes(effect.malId)
                is DetailEffect.NavigateToCharacters -> onNavigateToCharacters(effect.malId)
                is DetailEffect.NavigateToCharacterDetail -> onNavigateToCharacterDetail(effect.characterId)
                is DetailEffect.NavigateToPersonDetail -> onNavigateToPersonDetail(effect.personMalId)
                is DetailEffect.NavigateToStudioDetail -> onNavigateToStudioDetail(effect.studioMalId)
                is DetailEffect.NavigateToReviews -> onNavigateToReviews(effect.malId)
                DetailEffect.NavigateToReviewDetail -> onNavigateToReviewDetail()
            }
        }
    }

    DetailScreenContent(state = state, onBackClick = onBackClick, onEvent = viewModel::onEvent)
}

// DownloadManager (không phải tự đọc byte qua Coil rồi ghi MediaStore) — lưu
// vào thư mục Pictures công khai, có notification tải xong, Photos/Gallery
// tự quét thấy ảnh mới, đúng kỳ vọng "nút tải xuống" thông thường của user.
// Quyền WRITE_EXTERNAL_STORAGE (runtime request ở DetailScreen phía trên cho
// API 23-28, Manifest khai maxSdkVersion=28) đã được đảm bảo trước khi hàm
// này được gọi. Bắt SecurityException/IllegalArgumentException thay vì
// catch(Exception) rộng — đây là 2 loại lỗi thực tế có thể xảy ra (quyền bị
// thu hồi giữa chừng, URL dị dạng), không phải bắt mọi lỗi runtime.
private fun downloadPicture(context: Context, url: String) {
    try {
        val fileName = "AniMike_${System.currentTimeMillis()}.jpg"
        val request = DownloadManager.Request(url.toUri())
            .setTitle(fileName)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, fileName)
            .setMimeType("image/jpeg")
            .setAllowedOverMetered(true)
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
        Toast.makeText(context, "Đang tải ảnh xuống...", Toast.LENGTH_SHORT).show()
    } catch (e: SecurityException) {
        Log.w("DetailScreen", "Không có quyền tải ảnh xuống", e)
        Toast.makeText(context, "Không thể tải ảnh xuống", Toast.LENGTH_SHORT).show()
    } catch (e: IllegalArgumentException) {
        Log.w("DetailScreen", "URL ảnh không hợp lệ để tải xuống", e)
        Toast.makeText(context, "Không thể tải ảnh xuống", Toast.LENGTH_SHORT).show()
    }
}

// Ngưỡng hiện nút "cuộn lên đầu" — firstVisibleItemIndex tính theo SỐ ITEM
// (kể cả Spacer riêng), không phải theo section ngữ nghĩa, nên chọn ngưỡng
// hơi rộng để nút chỉ xuất hiện khi đã cuộn qua kha khá nội dung.
private const val SCROLL_TO_TOP_THRESHOLD = 6

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailScreenContent(
    state: DetailState,
    onBackClick: () -> Unit,
    onEvent: (DetailEvent) -> Unit,
) {
    // Hoist ở đây (không remember trong DetailContent) để nút "cuộn lên đầu"
    // — sibling của LazyColumn trong Box này — điều khiển được đúng scroll
    // state của nó (theo yêu cầu user: bỏ thu gọn/xem thêm cho Nhạc phim khi
    // gộp vào tab, thay bằng nút nổi cuộn về đầu trang).
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // contentWindowInsets = 0: AniMikeNavHost đã có Scaffold ngoài tiêu thụ
    // insets — tránh tiêu thụ 2 lần gây khoảng trắng dư quanh status bar.
    Scaffold(modifier = Modifier.fillMaxSize(), contentWindowInsets = WindowInsets(0)) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
        ) {
            when {
                // Cache-first: đã có detail (từ Room) thì luôn hiện, kể cả khi
                // đang refresh nền hoặc refresh vừa lỗi (stale-while-revalidate).
                // Pull-to-refresh (docs/ROADMAP.md mục 3b) — force refresh
                // detail + recommendations/reviews/pictures bất kể TTL. Episodes
                // không cần force vì vốn đã luôn gọi lại API ở mọi lần loadAll().
                state.detail != null -> PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { onEvent(DetailEvent.OnPullToRefresh) },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    DetailContent(
                        detail = state.detail,
                        characters = state.characters,
                        staff = state.staff,
                        recommendations = state.recommendations,
                        episodes = state.episodes,
                        reviews = state.reviews,
                        pictures = state.pictures,
                        themes = state.themes,
                        streamingLinks = state.streamingLinks,
                        // tabVideos: lọc bỏ video trùng với TrailerCard (xem
                        // comment ở DetailState.tabVideos).
                        videos = state.tabVideos,
                        listState = listState,
                        onEvent = onEvent,
                    )
                }

                state.isLoading -> LoadingContent()

                state.error != null -> ErrorContent(
                    message = state.error,
                    onRetry = { onEvent(DetailEvent.OnRetry) },
                )

                // Lưới an toàn: khoảng hở ngắn giữa lúc refresh xong (isLoading=false)
                // và lúc Flow từ Room re-emit detail — không để màn trắng thoáng qua.
                else -> LoadingContent()
            }

            // Neo cố định back/favorite trên cùng, KHÔNG đặt trong HeroHeader
            // (trước đây nằm trong LazyColumn's item nên cuộn mất theo nội
            // dung) — đặt ở đây, là sibling của LazyColumn trong Box này, nên
            // luôn nổi trên cùng bất kể cuộn tới đâu hay đang ở state nào.
            TopBar(
                isFavorite = state.isFavorite,
                showFavorite = state.detail != null,
                watchStatus = state.watchStatus,
                availableWatchStatuses = state.availableWatchStatuses,
                onBackClick = onBackClick,
                onFavoriteClick = { onEvent(DetailEvent.OnFavoriteClick) },
                onWatchStatusSelected = { onEvent(DetailEvent.OnWatchStatusSelected(it)) },
            )

            val showScrollToTop by remember {
                derivedStateOf { listState.firstVisibleItemIndex > SCROLL_TO_TOP_THRESHOLD }
            }
            AnimatedVisibility(
                visible = showScrollToTop,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(Dimens.ScreenPadding),
                // Cùng Motion token với AniMikeNavHost — Decelerate lúc vào,
                // Accelerate lúc ra (quy tắc "Enter/exit rule" của skill).
                enter = fadeIn(animationSpec = tween(Motion.DurationShort4, easing = Motion.EasingEmphasizedDecelerate)),
                exit = fadeOut(animationSpec = tween(Motion.DurationShort4, easing = Motion.EasingEmphasizedAccelerate)),
            ) {
                ScrollToTopButton(
                    onClick = { coroutineScope.launch { listState.animateScrollToItem(0) } },
                )
            }
        }
    }
}

@Composable
private fun TopBar(
    isFavorite: Boolean,
    showFavorite: Boolean,
    watchStatus: WatchStatus?,
    availableWatchStatuses: List<WatchStatus>,
    onBackClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onWatchStatusSelected: (WatchStatus) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Dimens.SpaceSm),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        BackButton(onClick = onBackClick)
        // Luôn render đủ 2 slot (Spacer cùng kích thước khi chưa có detail)
        // để Back không bị lệch vị trí phụ thuộc vào Arrangement.SpaceBetween
        // xử lý trường hợp 1-vs-2 con khác nhau. Nhóm hành động cá nhân
        // (trạng thái xem + yêu thích) gom về góc trên-phải — user góp ý màn
        // Detail đã quá nhiều section, card TrackingStatusBar cũ trong nội
        // dung bị gỡ, chuyển thành nút icon + DropdownMenu ở đây.
        if (showFavorite) {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
                WatchStatusButton(
                    currentStatus = watchStatus,
                    availableStatuses = availableWatchStatuses,
                    onStatusSelected = onWatchStatusSelected,
                )
                FavoriteButton(isFavorite = isFavorite, onClick = onFavoriteClick)
            }
        } else {
            Spacer(Modifier.size(Dimens.IconButtonSize))
        }
    }
}

// MVP6 Tracking — nút trạng thái xem trên TopBar (cạnh ♥, cùng style nền
// surface bán trong suốt): 🔖 trung tính khi chưa theo dõi, đổi thành emoji +
// màu ngữ nghĩa của trạng thái khi đã set (WatchStatusUi.kt). Bấm mở
// DropdownMenu (pattern M3 chuẩn cho action trên toolbar — thay card
// expand-inline cũ theo góp ý user vì Detail quá nhiều section). Menu chỉ
// liệt kê trạng thái HỢP LỆ theo tình trạng phát sóng (DetailState.
// availableWatchStatuses); chọn lại trạng thái đang set = bỏ theo dõi.
@Composable
private fun WatchStatusButton(
    currentStatus: WatchStatus?,
    availableStatuses: List<WatchStatus>,
    onStatusSelected: (WatchStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Menu là transient UI (đóng khi dismiss/chọn) — remember thường, không
    // cần saveable qua process death.
    var menuExpanded by remember { mutableStateOf(false) }
    val accent = currentStatus?.statusColor() ?: MaterialTheme.colorScheme.onBackground

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(Dimens.IconButtonSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                .clickable(onClick = { menuExpanded = true })
                .semantics {
                    contentDescription =
                        currentStatus?.let { "Trạng thái xem: ${it.label}" } ?: "Chọn trạng thái xem"
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = currentStatus?.emoji ?: "🔖",
                style = MaterialTheme.typography.titleMedium,
                color = accent,
            )
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            availableStatuses.forEach { status ->
                val selected = status == currentStatus
                val statusAccent = status.statusColor()
                DropdownMenuItem(
                    leadingIcon = {
                        Text(
                            text = status.emoji,
                            style = MaterialTheme.typography.labelLarge,
                            color = statusAccent,
                            // Label đã đủ nghĩa — ẩn glyph khỏi TalkBack.
                            modifier = Modifier.clearAndSetSemantics {},
                        )
                    },
                    text = {
                        Text(
                            text = status.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) statusAccent else MaterialTheme.colorScheme.onBackground,
                        )
                    },
                    trailingIcon = if (selected) {
                        {
                            Text(
                                text = "✓",
                                style = MaterialTheme.typography.labelLarge,
                                color = statusAccent,
                                modifier = Modifier.clearAndSetSemantics {},
                            )
                        }
                    } else {
                        null
                    },
                    // stateDescription thay cho glyph ✓ đã ẩn — TalkBack đọc
                    // "Đang chọn" cho item hiện tại.
                    modifier = if (selected) {
                        Modifier.semantics { stateDescription = "Đang chọn — bấm để bỏ theo dõi" }
                    } else {
                        Modifier
                    },
                    onClick = {
                        onStatusSelected(status)
                        menuExpanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun DetailContent(
    detail: AnimeDetail,
    characters: List<AnimeCharacter>,
    staff: List<AnimeStaffMember>,
    recommendations: List<Anime>,
    episodes: List<Episode>,
    reviews: List<AnimeReview>,
    pictures: List<Picture>,
    themes: AnimeThemes?,
    streamingLinks: List<StreamingLink>,
    videos: List<AnimeVideo>,
    listState: LazyListState,
    onEvent: (DetailEvent) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
        item { HeroHeader(detail = detail) }

        if (detail.genres.isNotEmpty()) {
            item { GenreChips(genres = detail.genres) }
        }

        // Studio ngay dưới genres (cùng nhóm "thông tin mô tả phim") — chip
        // bấm được mở Studio Detail (MVP5).
        if (detail.studios.isNotEmpty()) {
            item {
                StudiosRow(
                    studios = detail.studios,
                    onStudioClick = { onEvent(DetailEvent.OnStudioClick(it)) },
                )
            }
        }

        // "Xem trên..." đặt ngay dưới genres, trước trailer — hành động chính
        // của người muốn XEM phim (khác các section thông tin bên dưới), càng
        // gần đầu càng dễ thấy; chỉ 1 hàng chip cuộn ngang nên không làm dài
        // trang thêm đáng kể.
        item {
            StreamingLinksRow(
                links = streamingLinks,
                onLinkClick = { onEvent(DetailEvent.OnStreamingLinkClick(it)) },
            )
        }

        val trailerYoutubeId = detail.trailerYoutubeId
        if (trailerYoutubeId != null) {
            item {
                TrailerCard(
                    thumbnailUrl = detail.trailerThumbnailUrl,
                    onClick = { onEvent(DetailEvent.OnTrailerClick(trailerYoutubeId)) },
                )
            }
        }

        item { SynopsisSection(synopsis = detail.synopsis) }
        item { Spacer(Modifier.height(Dimens.SpaceLg)) }
        item {
            EpisodesSection(
                episodes = episodes,
                onSeeAllClick = { onEvent(DetailEvent.OnSeeAllEpisodesClick) },
            )
        }
        item { Spacer(Modifier.height(Dimens.SpaceLg)) }
        item {
            CharactersSection(
                characters = characters,
                onSeeAllClick = { onEvent(DetailEvent.OnSeeAllCharactersClick) },
                onCharacterClick = { onEvent(DetailEvent.OnCharacterClick(it)) },
            )
        }
        item { Spacer(Modifier.height(Dimens.SpaceLg)) }
        item {
            // MVP5 "Ê-kíp sản xuất" — đặt ngay sau "Nhân vật & Seiyuu" (cùng
            // nhóm "ai đứng sau bộ phim"), bấm 1 người mở People Detail.
            StaffSection(
                staff = staff,
                onStaffMemberClick = { onEvent(DetailEvent.OnStaffMemberClick(it)) },
            )
        }
        item { Spacer(Modifier.height(Dimens.SpaceLg)) }
        item { RelationsSection(relations = detail.relations) }
        item { Spacer(Modifier.height(Dimens.SpaceLg)) }
        // "Hình ảnh" đặt sau nhóm thông tin về chính bộ phim (tập/nhân vật/
        // liên quan), trước nhóm khám phá (Đề xuất/Đánh giá/Nhạc phim) — tính
        // năng phát sinh ngoài kit Animax, xem docs/ROADMAP.md MVP3.
        item {
            PicturesSection(
                pictures = pictures,
                onDownloadClick = { onEvent(DetailEvent.OnDownloadPictureClick(it)) },
            )
        }
        item { Spacer(Modifier.height(Dimens.SpaceLg)) }
        item {
            // "Nhạc phim" + "Video" (promo/MV) gộp vào tab cùng Đề xuất/Đánh
            // giá (theo yêu cầu user — Detail vốn đã nhiều section, gộp lại
            // đỡ dài trang). "Thống kê" ĐÃ CHUYỂN sang màn Đánh giá "Xem tất
            // cả" (ReviewsScreen) — xem docs/ROADMAP.md.
            ExploreTabsSection(
                recommendations = recommendations,
                reviews = reviews,
                themes = themes,
                videos = videos,
                onRecommendationClick = { onEvent(DetailEvent.OnRecommendationClick(it)) },
                onSeeAllReviewsClick = { onEvent(DetailEvent.OnSeeAllReviewsClick) },
                onReviewClick = { onEvent(DetailEvent.OnReviewClick(it)) },
                onVideoClick = { onEvent(DetailEvent.OnTrailerClick(it)) },
            )
        }
        item { Spacer(Modifier.height(Dimens.SpaceXl)) }
    }
}

@Composable
private fun HeroHeader(detail: AnimeDetail) {
    // ColorPainter không override equals() — remember để tránh AsyncImage coi
    // placeholder/error/fallback là "đổi" ở mỗi recomposition.
    val placeholderColor = MaterialTheme.colorScheme.surfaceVariant
    val placeholderPainter = remember(placeholderColor) { ColorPainter(placeholderColor) }
    val background = MaterialTheme.colorScheme.background

    Box(modifier = Modifier
        .fillMaxWidth()
        .height(Dimens.HeroHeaderHeight)) {
        AsyncImage(
            model = detail.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            placeholder = placeholderPainter,
            error = placeholderPainter,
            fallback = placeholderPainter,
            modifier = Modifier.fillMaxSize(),
        )
        // Gradient overlay #0B0E14 alpha 0->85% bottom-up (animike-design SKILL.md)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, background.copy(alpha = 0.85f)),
                    ),
                ),
        )
        // Back/Favorite giờ neo cố định ở DetailScreenContent.TopBar (nổi trên
        // toàn màn hình, không cuộn theo), không render lại ở đây nữa.

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
                if (detail.score != "N/A") HeroScoreBadge(score = detail.score)
                if (detail.isAiring) AiringPill()
            }
            Text(
                text = detail.title,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = detailMetaLine(detail),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// Studio ĐÃ TÁCH khỏi meta line (MVP5) — giờ là chip bấm được (StudiosRow)
// dưới GenreChips, không còn nhồi vào chuỗi text này.
private fun detailMetaLine(detail: AnimeDetail): String = listOfNotNull(
    detail.type,
    detail.year?.toString(),
    detail.episodes?.let { "$it tập" },
).joinToString(" · ")

// Style riêng cho hero header (nền surface alpha 80% đè trên ảnh cover, không
// phải nền primary như ScoreBadge trên AnimeCard — 2 ngữ cảnh khác nhau, xem
// animike-design SKILL.md mục Score badge).
@Composable
private fun HeroScoreBadge(score: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Dimens.RadiusChip))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
            .padding(horizontal = Dimens.SpaceSm, vertical = Dimens.SpaceXs),
    ) {
        Text(
            text = "★ $score",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.tertiary,
        )
    }
}

@Composable
private fun AiringPill() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Dimens.RadiusChip))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
            .padding(horizontal = Dimens.SpaceSm, vertical = Dimens.SpaceXs),
    ) {
        Text(
            text = "ĐANG CHIẾU",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.success,
        )
    }
}

@Composable
private fun FavoriteButton(isFavorite: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(Dimens.IconButtonSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
            .clickable(onClick = onClick)
            .semantics { contentDescription = if (isFavorite) "Bỏ yêu thích" else "Yêu thích" },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (isFavorite) "♥" else "♡",
            style = MaterialTheme.typography.titleMedium,
            color = if (isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun GenreChips(genres: List<String>) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        items(genres, key = { it }) { genre ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Dimens.RadiusChip))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = Dimens.SpaceMd, vertical = Dimens.SpaceXs),
            ) {
                Text(
                    text = genre,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// MVP5 — hàng chip studio bấm được (mở Studio Detail). Chip nền primary-nhạt
// + chữ primary như StreamingLinksRow để báo hiệu bấm được (khác GenreChips
// surfaceVariant thuần hiển thị).
@Composable
private fun StudiosRow(studios: List<Studio>, onStudioClick: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        Text(
            text = "Studio:",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        LazyRow(
            contentPadding = PaddingValues(vertical = Dimens.SpaceSm),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
        ) {
            items(studios, key = { it.malId }) { studio ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Dimens.RadiusChip))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .clickable(
                            onClickLabel = "Mở studio ${studio.name}",
                            role = Role.Button,
                            onClick = { onStudioClick(studio.malId) },
                        )
                        .padding(horizontal = Dimens.SpaceMd, vertical = Dimens.SpaceXs),
                ) {
                    Text(
                        text = studio.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

// MVP4 nút "Xem trên..." (/anime/{id}/streaming) — 1 hàng chip cuộn ngang mở
// nền tảng streaming hợp pháp bằng browser ngoài. Chip nền primary-nhạt +
// chữ primary (khác GenreChips surfaceVariant thuần hiển thị) để báo hiệu
// đây là hành động bấm được. AnimatedVisibility vì streaming tải SAU detail
// (cùng lý do CharactersSection).
@Composable
private fun StreamingLinksRow(links: List<StreamingLink>, onLinkClick: (String) -> Unit) {
    AnimatedVisibility(visible = links.isNotEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.ScreenPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
        ) {
            Text(
                text = "Xem trên:",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            LazyRow(
                contentPadding = PaddingValues(vertical = Dimens.SpaceSm),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            ) {
                items(links, key = { it.url }) { link ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(Dimens.RadiusChip))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .clickable(
                                onClickLabel = "Mở ${link.name}",
                                role = Role.Button,
                                onClick = { onLinkClick(link.url) },
                            )
                            .padding(horizontal = Dimens.SpaceMd, vertical = Dimens.SpaceXs),
                    ) {
                        Text(
                            text = link.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

// Thay nút text "Xem trailer" cũ bằng card thumbnail 16:9 + play overlay —
// vị trí tương ứng nút "Play" pill trong kit (32_Dark_anime episode details):
// trailer là nội dung video duy nhất app có (Jikan không có video tập phim,
// xem FEATURES.md mục 4), nên nó xứng đáng ngôn ngữ hình ảnh của một video.
// Bấm vào ĐIỀU HƯỚNG SANG APP YOUTUBE (không nhúng WebView trong app nữa) —
// YouTube đã siết chặn embed player từ phía họ (lỗi "Error 153", xảy ra diện
// rộng không riêng app này), nên nhúng qua WebView không còn đáng tin cậy.
// onClick chỉ gửi DetailEvent — startActivity() thật sự nằm ở DetailEffect.
// OpenYoutube xử lý trong LaunchedEffect của DetailScreen (như 4 effect điều
// hướng khác), không gọi thẳng trong composable này (quy tắc MVI CLAUDE.md).
@Composable
private fun TrailerCard(thumbnailUrl: String?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm)
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(Dimens.RadiusCard))
            .clickable(
                onClickLabel = "Xem trailer trên YouTube",
                role = Role.Button,
                onClick = onClick
            ),
    ) {
        val placeholderColor = MaterialTheme.colorScheme.surfaceVariant
        val placeholderPainter = remember(placeholderColor) { ColorPainter(placeholderColor) }
        AsyncImage(
            model = thumbnailUrl,
            // Trang trí — hành động đã có onClickLabel + label "Xem trailer" bên dưới.
            contentDescription = null,
            // Crop: thumbnail hqdefault là 4:3 kèm letterbox, crop vào khung
            // 16:9 sẽ tự cắt bỏ 2 bar đen trên/dưới.
            contentScale = ContentScale.Crop,
            placeholder = placeholderPainter,
            error = placeholderPainter,
            fallback = placeholderPainter,
            modifier = Modifier.fillMaxSize(),
        )
        // Gradient đáy (0->85%, khớp hero header) thay vì scrim phẳng cũ —
        // scrim phẳng 30% không đủ tương phản khi thumbnail nền sáng/trắng
        // (hqdefault của 1 số video chỉ là ảnh placeholder màu nhạt), label
        // "Xem trailer" bị chìm mất. Gradient đáy luôn đảm bảo vùng chữ tối
        // đủ, bất kể độ sáng của thumbnail.
        val background = MaterialTheme.colorScheme.background
        val gradient = remember(background) {
            Brush.verticalGradient(colors = listOf(Color.Transparent, background.copy(alpha = 0.85f)))
        }
        Box(modifier = Modifier
            .fillMaxSize()
            .background(gradient))
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(Dimens.IconButtonSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                // Variation selector ︎: ép render dạng text đơn sắc, một
                // số font/thiết bị sẽ hiện ▶ dạng emoji màu nếu thiếu nó.
                text = "▶︎",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                // clickable ở Box cha đã merge semantics + onClickLabel "Xem
                // trailer" — ẩn glyph này khỏi TalkBack để khỏi đọc thừa
                // "black right-pointing triangle".
                modifier = Modifier.clearAndSetSemantics {},
            )
        }
        Text(
            text = "Xem trailer",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(Dimens.SpaceMd),
        )
    }
}

@Composable
private fun SynopsisSection(synopsis: String) {
    Column(modifier = Modifier.padding(horizontal = Dimens.ScreenPadding)) {
        Text(
            text = "Nội dung",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        ExpandableText(
            text = synopsis,
            modifier = Modifier.padding(top = Dimens.SpaceXs),
            maxCollapsedLines = 4,
        )
    }
}

// Chỉ preview CHARACTERS_PREVIEW_LIMIT nhân vật đầu — /characters trả về
// TOÀN BỘ nhân vật 1 lần (không phân trang), anime dài có thể tới hàng nghìn
// (One Piece ~1477). "Xem tất cả" mở CharactersScreen riêng (Routes.CHARACTERS)
// có ô tìm kiếm local (lọc trong bộ nhớ, không cần gọi lại API).
private const val CHARACTERS_PREVIEW_LIMIT = 15

// AnimatedVisibility thay vì `if (characters.isEmpty()) return` — characters
// fetch xong SAU detail (gọi tuần tự trong DetailViewModel.loadAll), nên
// section này "mọc" ra giữa chừng khi đang xem màn; animate cho mượt thay vì
// giật cứng.
@Composable
private fun CharactersSection(
    characters: List<AnimeCharacter>,
    onSeeAllClick: () -> Unit,
    onCharacterClick: (Int) -> Unit,
) {
    AnimatedVisibility(visible = characters.isNotEmpty()) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.ScreenPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Nhân vật & Seiyuu",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (characters.size > CHARACTERS_PREVIEW_LIMIT) {
                    Text(
                        text = "Xem tất cả",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable(onClick = onSeeAllClick)
                            .padding(Dimens.SpaceXs),
                    )
                }
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
            ) {
                items(
                    characters.take(CHARACTERS_PREVIEW_LIMIT),
                    key = { it.malId },
                ) { character ->
                    CharacterItem(character, onClick = { onCharacterClick(character.malId) })
                }
            }
        }
    }
}

@Composable
private fun CharacterItem(character: AnimeCharacter, onClick: () -> Unit) {
    val placeholderColor = MaterialTheme.colorScheme.surfaceVariant
    val placeholderPainter = remember(placeholderColor) { ColorPainter(placeholderColor) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(Dimens.AvatarSize)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = character.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            placeholder = placeholderPainter,
            error = placeholderPainter,
            fallback = placeholderPainter,
            modifier = Modifier
                .size(Dimens.AvatarSize)
                .clip(CircleShape),
        )
        Text(
            text = character.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground,
            // minLines = maxLines = 2: luôn chiếm đúng 2 dòng dù tên thực tế
            // dài 1 hay 2 dòng — tránh item "nhấp nhô" cao thấp khác nhau
            // trong cùng 1 hàng LazyRow (cùng lý do với EpisodeItem bên dưới).
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Dimens.SpaceXs),
        )
        // Luôn render dòng này (rỗng nếu không có seiyuu) để mọi item cùng
        // chiều cao — không dùng voiceActorName?.let {} nữa (ẩn/hiện có điều
        // kiện chính là nguyên nhân nhấp nhô).
        Text(
            text = character.voiceActorName ?: "",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

// MVP5 "Ê-kíp sản xuất" (/anime/{id}/staff) — cùng bố cục CharactersSection
// nhưng KHÔNG có "Xem tất cả" (chưa có màn riêng, response vốn không phân
// trang nên preview đã là toàn bộ danh sách). AnimatedVisibility cùng lý do.
@Composable
private fun StaffSection(staff: List<AnimeStaffMember>, onStaffMemberClick: (Int) -> Unit) {
    AnimatedVisibility(visible = staff.isNotEmpty()) {
        Column {
            Text(
                text = "Ê-kíp sản xuất",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
            ) {
                items(staff, key = { it.personMalId }) { member ->
                    StaffMemberItem(member, onClick = { onStaffMemberClick(member.personMalId) })
                }
            }
        }
    }
}

@Composable
private fun StaffMemberItem(member: AnimeStaffMember, onClick: () -> Unit) {
    val placeholderColor = MaterialTheme.colorScheme.surfaceVariant
    val placeholderPainter = remember(placeholderColor) { ColorPainter(placeholderColor) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(Dimens.AvatarSize)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = member.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            placeholder = placeholderPainter,
            error = placeholderPainter,
            fallback = placeholderPainter,
            modifier = Modifier
                .size(Dimens.AvatarSize)
                .clip(CircleShape),
        )
        Text(
            text = member.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Dimens.SpaceXs),
        )
        // 1 người có thể giữ nhiều vai trò (VD ["Director", "Storyboard"]) —
        // ghép lại 1 dòng, luôn render (rỗng nếu thiếu) để cùng chiều cao,
        // cùng lý do dòng seiyuu của CharacterItem.
        Text(
            text = member.positions.joinToString(", "),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

// Chỉ preview EPISODES_PREVIEW_LIMIT tập đầu trong list ngang — anime dài có
// thể có hàng chục/trăm tập, hiện hết ở LazyRow là UX tệ. "Xem tất cả" mở màn
// EpisodesScreen riêng (Routes.EPISODES) hiện đủ danh sách đã fetch.
private const val EPISODES_PREVIEW_LIMIT = 10

// AnimatedVisibility thay vì early return — cùng lý do với CharactersSection
// (episodes fetch xong sau detail, "mọc" ra giữa chừng khi đang xem màn).
@Composable
private fun EpisodesSection(episodes: List<Episode>, onSeeAllClick: () -> Unit) {
    AnimatedVisibility(visible = episodes.isNotEmpty()) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.ScreenPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Các tập",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (episodes.size > EPISODES_PREVIEW_LIMIT) {
                    Text(
                        text = "Xem tất cả",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable(onClick = onSeeAllClick)
                            .padding(Dimens.SpaceXs),
                    )
                }
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            ) {
                items(episodes.take(EPISODES_PREVIEW_LIMIT), key = { it.number }) { episode -> EpisodeItem(episode) }
            }
        }
    }
}

@Composable
private fun EpisodeItem(episode: Episode) {
    val placeholderColor = MaterialTheme.colorScheme.surfaceVariant
    val placeholderPainter = remember(placeholderColor) { ColorPainter(placeholderColor) }

    Column(modifier = Modifier.width(Dimens.EpisodeCardWidth)) {
        AsyncImage(
            model = episode.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            placeholder = placeholderPainter,
            error = placeholderPainter,
            fallback = placeholderPainter,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(Dimens.RadiusCard)),
        )
        Text(
            text = "Tập ${episode.number}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = Dimens.SpaceXs),
        )
        Text(
            text = episode.title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground,
            // minLines = maxLines = 2: luôn chiếm đúng 2 dòng dù title thực tế
            // dài 1 hay 2 dòng — tránh card "nhấp nhô" cao thấp khác nhau trong
            // cùng 1 hàng LazyRow.
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RelationsSection(relations: List<RelationGroup>) {
    if (relations.isEmpty()) return
    Column(
        modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
    ) {
        Text(
            text = "Liên quan",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        relations.forEach { group ->
            // key(): app dùng stale-while-revalidate — nếu detail.relations đổi
            // hình dạng giữa các lần refresh nền, remember theo vị trí slot có
            // thể gán nhầm trạng thái expanded/isOverflowing cũ cho nội dung mới.
            key(group.relation) {
                ExpandableText(text = "${group.relation}: ${group.titles.joinToString(", ")}")
            }
        }
    }
}

// Gộp openings+endings thành 1 danh sách chung — MVP4 "Nhạc OP/ED" giờ là 1
// tab trong ExploreTabsSection (cùng Đề xuất/Đánh giá) thay vì section riêng
// đứng 1 mình, theo yêu cầu user (Detail vốn đã nhiều section). Không còn
// thu gọn/xem thêm — thay bằng nút nổi "cuộn lên đầu" ở cấp DetailScreenContent
// khi list dài (One Piece, Naruto... có thể 15-20+ OP/ED).
private data class ThemeEntry(val label: String, val text: String)

private fun buildThemeEntries(themes: AnimeThemes?): List<ThemeEntry> {
    val current = themes ?: return emptyList()
    return current.openings.map { ThemeEntry("Mở đầu", it) } + current.endings.map { ThemeEntry("Kết thúc", it) }
}

@Composable
private fun ThemesList(entries: List<ThemeEntry>) {
    Column(
        modifier = Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
    ) {
        entries.forEach { entry -> ThemeRow(label = entry.label, text = entry.text) }
    }
}

@Composable
private fun ThemeRow(label: String, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
        Text(
            text = "♪ $label:",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Tab "Video" (MVP4, /anime/{id}/videos: promo/PV + music video) — LazyRow
// card 16:9 kiểu EpisodeItem nhưng kèm play overlay như TrailerCard (đây là
// video mở YouTube, không phải thông tin thuần). Bấm card gửi cùng event
// OnTrailerClick (cùng hành vi "mở 1 video YouTube").
@Composable
private fun VideosRow(videos: List<AnimeVideo>, onVideoClick: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm),
        horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap),
    ) {
        items(videos, key = { it.youtubeId }) { video -> VideoCard(video = video, onClick = { onVideoClick(video.youtubeId) }) }
    }
}

@Composable
private fun VideoCard(video: AnimeVideo, onClick: () -> Unit) {
    val placeholderColor = MaterialTheme.colorScheme.surfaceVariant
    val placeholderPainter = remember(placeholderColor) { ColorPainter(placeholderColor) }
    Column(
        modifier = Modifier
            .width(Dimens.VideoCardWidth)
            .clickable(
                onClickLabel = "Xem ${video.title} trên YouTube",
                role = Role.Button,
                onClick = onClick
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(Dimens.RadiusCard)),
        ) {
            AsyncImage(
                model = video.thumbnailUrl,
                // Trang trí — hành động đã có onClickLabel + title bên dưới.
                contentDescription = null,
                // Crop: thumbnail hqdefault 4:3 kèm letterbox, crop vào khung
                // 16:9 tự cắt bỏ 2 bar đen (cùng lý do TrailerCard).
                contentScale = ContentScale.Crop,
                placeholder = placeholderPainter,
                error = placeholderPainter,
                fallback = placeholderPainter,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(Dimens.IconButtonSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    // Variation selector — cùng lý do với TrailerCard.
                    text = "▶︎",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.clearAndSetSemantics {},
                )
            }
        }
        Text(
            text = video.title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground,
            // minLines = maxLines: card cùng chiều cao trong 1 hàng LazyRow —
            // cùng lý do với EpisodeItem/CharacterItem.
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = Dimens.SpaceXs),
        )
        // Luôn render (rỗng nếu là promo không có subtitle) — mọi card cùng
        // chiều cao, cùng lý do với dòng seiyuu của CharacterItem.
        Text(
            text = video.subtitle ?: "",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// Bộ sưu tập ảnh từ /pictures (poster art các thời kỳ) — tính năng phát sinh
// ngoài kit Animax, không có mockup: LazyRow poster 2:3 (khớp AnimeCard),
// bấm ảnh mở viewer full-screen vuốt ngang. AnimatedVisibility vì pictures
// là lệnh gọi phụ cuối cùng, "mọc" ra sau khi detail đã hiển thị. Grid dùng
// thumbnailUrl (nhỏ hơn) — viewer full-screen mới dùng fullUrl (nét nhất),
// xem domain.model.Picture.
@Composable
private fun PicturesSection(pictures: List<Picture>, onDownloadClick: (String) -> Unit) {
    // Index ảnh đang mở trong viewer, null = viewer đóng — state thuần UI
    // (giống expanded của ExpandableText), không cần đưa vào DetailState.
    // rememberSaveable: giữ viewer mở đúng ảnh khi xoay màn hình. Đặt NGOÀI
    // AnimatedVisibility để lifecycle viewer không phụ thuộc animation ẩn/hiện.
    var viewerIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    AnimatedVisibility(visible = pictures.isNotEmpty()) {
        Column {
            SectionTitle("Hình ảnh")
            LazyRow(
                contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm),
                horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap),
            ) {
                itemsIndexed(pictures, key = { _, picture -> picture.fullUrl }) { index, picture ->
                    val placeholderColor = MaterialTheme.colorScheme.surfaceVariant
                    val placeholderPainter = remember(placeholderColor) { ColorPainter(placeholderColor) }
                    AsyncImage(
                        model = picture.thumbnailUrl,
                        contentDescription = "Ảnh ${index + 1}",
                        contentScale = ContentScale.Crop,
                        placeholder = placeholderPainter,
                        error = placeholderPainter,
                        fallback = placeholderPainter,
                        modifier = Modifier
                            .width(Dimens.CardWidth)
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(Dimens.RadiusCard))
                            .clickable(onClickLabel = "Xem ảnh lớn") { viewerIndex = index },
                    )
                }
            }
        }
    }

    // Guard pictures.isNotEmpty + coerceIn: sau process death, viewerIndex
    // được rememberSaveable khôi phục TRƯỚC khi pictures fetch lại xong —
    // không guard sẽ mở viewer rỗng / index vượt biên trong lúc chờ.
    val index = viewerIndex
    if (index != null && pictures.isNotEmpty()) {
        PictureViewerDialog(
            pictures = pictures,
            initialPage = index.coerceIn(0, pictures.lastIndex),
            onDismiss = { viewerIndex = null },
            onDownloadClick = onDownloadClick,
        )
    }
}

@Composable
private fun PictureViewerDialog(
    pictures: List<Picture>,
    initialPage: Int,
    onDismiss: () -> Unit,
    onDownloadClick: (String) -> Unit,
) {
    // usePlatformDefaultWidth = false: dialog chiếm trọn màn hình cho viewer
    // ảnh, thay vì bị bó trong khung dialog mặc định.
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            val pagerState = rememberPagerState(initialPage = initialPage) { pictures.size }
            val placeholderColor = MaterialTheme.colorScheme.surfaceVariant
            val placeholderPainter = remember(placeholderColor) { ColorPainter(placeholderColor) }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                // Preload trang kề: ảnh large nặng, không preload thì mỗi lần
                // vuốt user phải nhìn placeholder chờ tải từ đầu.
                beyondViewportPageCount = 1,
                key = { pictures[it].fullUrl },
            ) { page ->
                AsyncImage(
                    // fullUrl (large_image_url ưu tiên) — viewer LUÔN hiện
                    // đúng bản nét nhất, theo yêu cầu user (khác grid dùng
                    // thumbnailUrl nhỏ hơn phía trên).
                    model = pictures[page].fullUrl,
                    contentDescription = "Ảnh ${page + 1}",
                    // Fit (không Crop): viewer phải thấy TRỌN ảnh gốc, đây là
                    // mục đích của việc phóng to.
                    contentScale = ContentScale.Fit,
                    placeholder = placeholderPainter,
                    error = placeholderPainter,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Dimens.ScreenPadding),
                )
            }

            // Toolbar trên cùng: tải xuống (start) + đóng (end) — bố cục
            // chuẩn "back 1 bên, action 1 bên" quen thuộc của trình xem ảnh
            // (Google Photos...), cùng ngôn ngữ hình ảnh (tròn, nền mờ) với
            // BackButton.
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(Dimens.SpaceSm)
                    .size(Dimens.IconButtonSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                    .clickable(onClick = { onDownloadClick(pictures[pagerState.currentPage].fullUrl) })
                    .semantics { contentDescription = "Tải ảnh xuống" },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "⬇",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.clearAndSetSemantics {},
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(Dimens.SpaceSm)
                    .size(Dimens.IconButtonSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                    .clickable(onClick = onDismiss)
                    .semantics { contentDescription = "Đóng" },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "✕",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    // Box cha đã có contentDescription "Đóng" — ẩn glyph khỏi
                    // TalkBack để khỏi đọc thừa "Đóng, multiplication sign".
                    modifier = Modifier.clearAndSetSemantics {},
                )
            }

            Text(
                text = "${pagerState.currentPage + 1}/${pictures.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(Dimens.ScreenPadding),
            )
        }
    }
}

// Kit Animax: tab "More Like This" / "Comments" (underline indicator). Ở đây
// đổi tên "Comments" thành "Đánh giá" vì Jikan chỉ có reviews MAL, không có
// bình luận thời gian thực. Thêm "Nhạc phim" (tab 3) + "Video" (tab 4 — MVP4
// promo/PV + music video, cùng lý do gộp: đỡ dài Detail). Chỉ hiện TabRow
// khi ≥2 tab có dữ liệu — nếu chỉ 1 tab có, hiện thẳng section đó (không cần
// tab để chọn 1 lựa chọn duy nhất); nếu tất cả đều rỗng, ẩn hẳn.
private enum class DetailTab { RECOMMENDATIONS, REVIEWS, THEMES, VIDEOS }

private fun detailTabLabel(tab: DetailTab): String = when (tab) {
    DetailTab.RECOMMENDATIONS -> "Đề xuất"
    DetailTab.REVIEWS -> "Đánh giá"
    DetailTab.THEMES -> "Nhạc phim"
    DetailTab.VIDEOS -> "Video"
}

@Composable
private fun ExploreTabsSection(
    recommendations: List<Anime>,
    reviews: List<AnimeReview>,
    themes: AnimeThemes?,
    videos: List<AnimeVideo>,
    onRecommendationClick: (Int) -> Unit,
    onSeeAllReviewsClick: () -> Unit,
    onReviewClick: (AnimeReview) -> Unit,
    onVideoClick: (String) -> Unit,
) {
    val themeEntries = remember(themes) { buildThemeEntries(themes) }
    // buildList: chỉ liệt kê tab THỰC SỰ có data — khác bản 2 tab cũ (so sánh
    // cứng 2 điều kiện), giờ tổng quát cho N tab để dễ mở rộng sau này.
    val availableTabs = remember(recommendations, reviews, themeEntries, videos) {
        buildList {
            if (recommendations.isNotEmpty()) add(DetailTab.RECOMMENDATIONS)
            if (reviews.isNotEmpty()) add(DetailTab.REVIEWS)
            if (themeEntries.isNotEmpty()) add(DetailTab.THEMES)
            if (videos.isNotEmpty()) add(DetailTab.VIDEOS)
        }
    }
    // AnimatedVisibility thay vì early return khi cả 3 đều rỗng — cùng lý do
    // với CharactersSection/EpisodesSection (data fetch xong sau detail,
    // "mọc" ra giữa chừng khi đang xem màn).
    AnimatedVisibility(visible = availableTabs.isNotEmpty()) {
        when {
            availableTabs.size >= 2 -> {
                var selectedTab by rememberSaveable { mutableStateOf(availableTabs.first()) }
                // Phòng trường hợp hiếm: tab đang chọn rớt khỏi availableTabs
                // giữa chừng (SWR refresh làm 1 nguồn data đang chọn về rỗng)
                // — fallback về tab đầu tiên còn lại thay vì render tab không tồn tại.
                val effectiveTab = if (selectedTab in availableTabs) selectedTab else availableTabs.first()
                Column {
                    DetailTabRow(
                        tabs = availableTabs,
                        selectedTab = effectiveTab,
                        onTabSelected = { selectedTab = it },
                    )
                    // Crossfade khi đổi tab thay vì "nhảy" cứng giữa nội dung
                    // khác hình dạng (LazyRow vs Column cao thấp khác nhau).
                    // SizeTransform(clip = false) — nếu không, nội dung cao hơn
                    // bị cắt trong lúc animate vì AnimatedContent mặc định clip
                    // theo size nội suy (compose-expert/animation.md mục tab-switch).
                    AnimatedContent(
                        targetState = effectiveTab,
                        // Cùng Motion token với crossfade tab-switch của
                        // AniMikeNavHost (DurationShort4 + Decelerate/Accelerate).
                        transitionSpec = {
                            val enterSpec = tween<Float>(Motion.DurationShort4, easing = Motion.EasingEmphasizedDecelerate)
                            val exitSpec = tween<Float>(Motion.DurationShort4, easing = Motion.EasingEmphasizedAccelerate)
                            fadeIn(animationSpec = enterSpec) togetherWith
                                fadeOut(animationSpec = exitSpec) using SizeTransform(clip = false)
                        },
                        label = "detail_tab_content",
                    ) { tab ->
                        when (tab) {
                            DetailTab.RECOMMENDATIONS -> RecommendationsRow(recommendations, onRecommendationClick)
                            DetailTab.REVIEWS -> ReviewsList(reviews, onReviewClick, onSeeAllReviewsClick)
                            DetailTab.THEMES -> ThemesList(themeEntries)
                            DetailTab.VIDEOS -> VideosRow(videos, onVideoClick)
                        }
                    }
                }
            }

            availableTabs.size == 1 -> Column {
                val onlyTab = availableTabs.first()
                SectionTitle(
                    // Chỉ RECOMMENDATIONS cần tiêu đề khác nhãn tab ("Đề xuất
                    // tương tự" đầy đủ nghĩa hơn khi đứng 1 mình) — còn lại
                    // dùng lại detailTabLabel, tránh duplicate chuỗi (phát
                    // hiện qua review, sửa).
                    if (onlyTab == DetailTab.RECOMMENDATIONS) "Đề xuất tương tự" else detailTabLabel(onlyTab),
                )
                when (onlyTab) {
                    DetailTab.RECOMMENDATIONS -> RecommendationsRow(recommendations, onRecommendationClick)
                    DetailTab.REVIEWS -> ReviewsList(reviews, onReviewClick, onSeeAllReviewsClick)
                    DetailTab.THEMES -> ThemesList(themeEntries)
                    DetailTab.VIDEOS -> VideosRow(videos, onVideoClick)
                }
            }
            // Tất cả đều rỗng: AnimatedVisibility(visible=false) đã lo phần ẩn,
            // nhánh này chỉ để tường minh chứ không có gì phải render.
            else -> Unit
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

// Dùng TabRow/Tab chuẩn Material3 thay vì tự vẽ Row + gạch chân (bản tự vẽ
// trước đó thiếu track nền + không animate, nhìn "thô") — TabRow tự lo phần
// đo width chia đều và vẽ indicator trượt mượt giữa các tab. `tabs` là danh
// sách ĐỘNG (chỉ những tab thực sự có data) nên dùng index trong list này
// làm selectedTabIndex, KHÔNG dùng DetailTab.ordinal (lệch khi thiếu tab).
@Composable
private fun DetailTabRow(tabs: List<DetailTab>, selectedTab: DetailTab, onTabSelected: (DetailTab) -> Unit) {
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
                    Text(
                        text = detailTabLabel(tab),
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecommendationsRow(recommendations: List<Anime>, onClick: (Int) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm),
        horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap),
    ) {
        items(recommendations, key = { it.malId }) { anime ->
            AnimeCard(
                anime = anime,
                onClick = { onClick(anime.malId) },
                modifier = Modifier.width(Dimens.CardWidth),
            )
        }
    }
}

// reviews đã bị giới hạn số lượng ở AnimeDetailRepository.getReviews() (chỉ
// preview) — luôn hiện "Xem tất cả" vì preview luôn bị cắt, khả năng còn thêm
// review khác qua ReviewsScreen (Paging 3) là gần như chắc chắn.
// Dùng chung ReviewCard với ReviewsScreen (theo yêu cầu user: đồng bộ hiển
// thị) — bấm 1 card mở ReviewDetailScreen xem đầy đủ, cùng hành vi click ở
// ReviewsScreen (xem DetailContract.OnReviewClick).
@Composable
private fun ReviewsList(reviews: List<AnimeReview>, onReviewClick: (AnimeReview) -> Unit, onSeeAllClick: () -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
    ) {
        reviews.forEach { review ->
            key(review.id) { ReviewCard(review = review, onClick = { onReviewClick(review) }) }
        }
        // "Xem tất cả" đặt DƯỚI danh sách (khác các section LazyRow ngang có
        // nút ở header) — với list dọc, đọc hết preview rồi mới tới lời mời
        // xem thêm thuận mắt hơn.
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text(
                text = "Xem tất cả",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(onClick = onSeeAllClick)
                    .padding(Dimens.SpaceXs),
            )
        }
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
