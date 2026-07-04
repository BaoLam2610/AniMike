package com.lambao.animike.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.lambao.animike.data.remote.JikanApi
import com.lambao.animike.domain.mapper.toDomain
import com.lambao.animike.domain.model.Episode
import retrofit2.HttpException
import java.io.IOException

/**
 * Chỉ bắt HttpException/IOException — không catch(Exception) rộng, đúng
 * anti-pattern list trong compose-expert/references/paging-mvi-testing.md.
 * Dùng cho màn EpisodesScreen (xem tất cả) — preview trong Detail vẫn dùng
 * AnimeDetailRepository.getEpisodes() one-shot (chỉ cần 10 tập đầu).
 */
class AnimeEpisodesPagingSource(
    private val api: JikanApi,
    private val malId: Int,
) : PagingSource<Int, Episode>() {

    // Anime rất dài (nhiều chục trang) có rủi ro 2 trang liền kề trả trùng 1
    // tập (lệch ranh giới phân trang phía server), gây "Key ... was already
    // used" khi dùng episode.number làm itemKey (LazyVerticalGrid). Set này
    // sống suốt vòng đời PagingSource (1 instance/lần refresh) để khử trùng
    // xuyên trang, không chỉ trong 1 trang.
    private val seenEpisodeNumbers = mutableSetOf<Int>()

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Episode> {
        val page = params.key ?: 1
        return try {
            val response = api.getEpisodes(malId, page)
            // seenEpisodeNumbers.add() trả false cho số đã gặp — khử trùng luôn
            // cả trong-trang lẫn xuyên-trang trong 1 lượt filter.
            val episodes = response.data
                .map { it.toDomain() }
                .filter { seenEpisodeNumbers.add(it.number) }
            LoadResult.Page(
                data = episodes,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (response.pagination?.hasNextPage == true) page + 1 else null,
            )
        } catch (e: HttpException) {
            LoadResult.Error(e)
        } catch (e: IOException) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Episode>): Int? =
        state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
}
