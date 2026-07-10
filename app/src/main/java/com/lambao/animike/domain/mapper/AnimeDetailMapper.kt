package com.lambao.animike.domain.mapper

import com.lambao.animike.data.remote.dto.AnimeFullDto
import com.lambao.animike.data.remote.dto.TrailerDto
import com.lambao.animike.domain.model.AnimeDetail
import com.lambao.animike.domain.model.RelationGroup
import com.lambao.animike.domain.model.Studio
import java.util.Locale

// Ký tự hợp lệ của 1 video id YouTube — dùng để validate CẢ field youtube_id
// thô lẫn id rút ra từ URL, vì id này sẽ nối thẳng vào
// "https://www.youtube.com/watch?v={id}" rồi mở qua Intent (DetailEffect.
// OpenYoutube) — không sanitize thì 1 giá trị dị dạng từ Jikan có thể tạo
// ra URI hỏng.
private const val YOUTUBE_ID_CHARS = """[A-Za-z0-9_-]{5,}"""
private val youtubeIdOnlyRegex = Regex("""^$YOUTUBE_ID_CHARS$""")

// Khớp id trong ".../embed/{id}?..." (embed_url), ".../watch?v={id}" (url),
// và ".../youtu.be/{id}" (dạng rút gọn, phòng xa dù Jikan chưa thấy trả kiểu
// này). Id YouTube thực tế luôn 11 ký tự nhưng nới xuống 5+ ký tự cho an toàn.
private val youtubeIdInUrlRegex = Regex("""(?:embed/|v=|youtu\.be/)($YOUTUBE_ID_CHARS)""")

// youtube_id đôi khi null dù trailer tồn tại (VD anime 38524 chỉ có embed_url)
// — rút id từ embed_url/url làm fallback để nút trailer không biến mất oan.
// internal (không private): AnimeVideosMapper tái dùng cho promo/music video
// của /anime/{id}/videos — cùng shape TrailerDto, cùng data quirk.
internal fun TrailerDto.resolveYoutubeId(): String? =
    youtubeId?.takeIf { youtubeIdOnlyRegex.matches(it) }
        ?: listOfNotNull(embedUrl, url)
            .firstNotNullOfOrNull { youtubeIdInUrlRegex.find(it)?.groupValues?.get(1) }

fun AnimeFullDto.toDomain(): AnimeDetail = AnimeDetail(
    malId = malId,
    title = titleEnglish ?: title ?: "Không rõ tên",
    imageUrl = images?.jpg?.largeImageUrl ?: images?.jpg?.imageUrl,
    trailerYoutubeId = trailer?.resolveYoutubeId(),
    score = score?.let { String.format(Locale.US, "%.1f", it) } ?: "N/A",
    rank = rank?.let { "#$it" } ?: "N/A",
    type = type,
    episodes = episodes,
    year = year,
    status = status ?: "N/A",
    isAiring = airing ?: false,
    // Chỉ giữ studio có ĐỦ malId + name (bấm được) — bỏ studio thiếu id vì
    // không mở được /producers/{id}/full. distinctBy phòng Jikan trả trùng.
    studios = studios.mapNotNull { dto ->
        val id = dto.malId ?: return@mapNotNull null
        val name = dto.name?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        Studio(malId = id, name = name)
    }.distinctBy { it.malId },
    genres = genres.mapNotNull { it.name },
    synopsis = synopsis ?: "Chưa có mô tả.",
    relations = relations.mapNotNull { rel ->
        val animeTitles = rel.entry.filter { it.type == "anime" }.mapNotNull { it.name }
        if (animeTitles.isEmpty()) null else RelationGroup(relation = rel.relation ?: "", titles = animeTitles)
    },
)
