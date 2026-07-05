package com.lambao.animike.ui.detail

import androidx.compose.runtime.Immutable
import com.lambao.animike.domain.model.Anime
import com.lambao.animike.domain.model.AnimeCharacter
import com.lambao.animike.domain.model.AnimeDetail
import com.lambao.animike.domain.model.AnimeReview
import com.lambao.animike.domain.model.AnimeThemes
import com.lambao.animike.domain.model.AnimeVideo
import com.lambao.animike.domain.model.Episode
import com.lambao.animike.domain.model.Picture
import com.lambao.animike.domain.model.StreamingLink

@Immutable
data class DetailState(
    val isLoading: Boolean = true,
    val detail: AnimeDetail? = null,
    val error: String? = null,
    val characters: List<AnimeCharacter> = emptyList(),
    val recommendations: List<Anime> = emptyList(),
    val episodes: List<Episode> = emptyList(),
    val reviews: List<AnimeReview> = emptyList(),
    // Ảnh từ /pictures (poster art các thời kỳ) — thumbnailUrl/fullUrl riêng
    // (xem domain.model.Picture) để viewer full-screen hiện đúng bản nét nhất.
    val pictures: List<Picture> = emptyList(),
    // MVP4 "Nhạc OP/ED" — null khi chưa tải xong hoặc anime không có OP/ED
    // (khác 4 mục trên vì là single object/anime, không phải list nên không
    // dùng emptyList() làm mặc định). "Thống kê" ĐÃ CHUYỂN sang màn Đánh giá
    // "Xem tất cả" (ReviewsContract) — xem docs/ROADMAP.md, không còn ở đây.
    val themes: AnimeThemes? = null,
    // MVP4 nút "Xem trên..." (/streaming) + tab "Video" (/videos: promo + MV).
    val streamingLinks: List<StreamingLink> = emptyList(),
    val videos: List<AnimeVideo> = emptyList(),
    // Review đang mở ở màn chi tiết (ReviewDetailScreen, dùng chung với
    // ReviewsScreen) — xem comment ở ReviewDetailScreen.kt.
    val selectedReview: AnimeReview? = null,
    val isFavorite: Boolean = false,
    // Pull-to-refresh (docs/ROADMAP.md mục 3b) — force refresh detail +
    // recommendations/reviews/pictures bất kể TTL. Episodes không cần "force"
    // vì nó vốn đã luôn gọi lại API ở mọi lần loadAll().
    val isRefreshing: Boolean = false,
) {
    // Video đầu tiên trong /videos.promo CHÍNH LÀ /full.trailer — verify qua
    // curl (anime 1: cả 2 cùng embed_url youtube gY5nDXOtv_o): Jikan trả
    // trùng 1 video ở cả 2 endpoint. TrailerCard đã hiện video này riêng nên
    // lọc bỏ khỏi tab "Video" để tránh trùng — vẫn giữ các promo/MV KHÁC
    // (PV 2, PV 3, music video...).
    val tabVideos: List<AnimeVideo>
        get() = videos.filter { it.youtubeId != detail?.trailerYoutubeId }
}

sealed interface DetailEvent {
    data object OnRetry : DetailEvent
    data object OnPullToRefresh : DetailEvent
    // youtubeId truyền thẳng từ call site (TrailerCard + item tab "Video" —
    // cả 2 cùng hành vi "mở 1 video YouTube") — đỡ phải đọc lại state trong
    // ViewModel.
    data class OnTrailerClick(val youtubeId: String) : DetailEvent
    // Nút "Xem trên..." — mở URL nền tảng streaming bằng browser ngoài.
    data class OnStreamingLinkClick(val url: String) : DetailEvent
    data object OnFavoriteClick : DetailEvent
    data class OnRecommendationClick(val malId: Int) : DetailEvent
    data object OnSeeAllEpisodesClick : DetailEvent
    data object OnSeeAllCharactersClick : DetailEvent
    data object OnSeeAllReviewsClick : DetailEvent
    // Bấm 1 ReviewCard trong tab "Đánh giá" — mở ReviewDetailScreen xem đầy
    // đủ (cùng hành vi click ở ReviewsScreen, xem ReviewDetailScreen.kt).
    data class OnReviewClick(val review: AnimeReview) : DetailEvent
    // Nút tải xuống trong viewer ảnh full-screen — url LUÔN là fullUrl (ảnh
    // nét nhất), không phải thumbnailUrl.
    data class OnDownloadPictureClick(val url: String) : DetailEvent
}

sealed interface DetailEffect {
    // Điều hướng RA NGOÀI app (startActivity) — phải đi qua Effect như mọi
    // side-effect khác, không gọi thẳng trong composable (quy tắc MVI ở
    // CLAUDE.md: composable chỉ render state + gửi event).
    data class OpenYoutube(val videoId: String) : DetailEffect
    // Mở URL bất kỳ bằng browser ngoài (nút "Xem trên...") — tách khỏi
    // OpenYoutube vì bên đó build URL YouTube từ videoId đã sanitize, còn
    // đây là URL nguyên bản từ Jikan.
    data class OpenExternalUrl(val url: String) : DetailEffect
    data class NavigateToDetail(val malId: Int) : DetailEffect
    data class NavigateToEpisodes(val malId: Int) : DetailEffect
    data class NavigateToCharacters(val malId: Int) : DetailEffect
    data class NavigateToReviews(val malId: Int) : DetailEffect
    data object NavigateToReviewDetail : DetailEffect
    // Tải ảnh xuống máy qua DownloadManager (nút tải trong viewer full-screen).
    data class DownloadPicture(val url: String) : DetailEffect
}
