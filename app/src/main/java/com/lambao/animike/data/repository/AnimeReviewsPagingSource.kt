package com.lambao.animike.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.lambao.animike.data.remote.JikanApi
import com.lambao.animike.domain.mapper.toDomain
import com.lambao.animike.domain.model.AnimeReview
import retrofit2.HttpException
import java.io.IOException

/**
 * Chỉ bắt HttpException/IOException — không catch(Exception) rộng, đúng
 * anti-pattern list trong compose-expert/references/paging-mvi-testing.md.
 * Dùng cho màn ReviewsScreen (xem tất cả) — preview trong Detail vẫn dùng
 * AnimeDetailRepository.getReviews() one-shot (chỉ cần REVIEWS_LIMIT đầu).
 */
class AnimeReviewsPagingSource(
    private val api: JikanApi,
    private val malId: Int,
) : PagingSource<Int, AnimeReview>() {

    // Cùng lý do với AnimeEpisodesPagingSource — khử trùng xuyên trang tránh
    // "Key ... was already used" nếu 2 trang liền kề lệch ranh giới server.
    private val seenReviewIds = mutableSetOf<Int>()

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, AnimeReview> {
        val page = params.key ?: 1
        return try {
            val response = api.getReviews(malId, page)
            val reviews = response.data
                .map { it.toDomain() }
                .filter { seenReviewIds.add(it.id) }
            LoadResult.Page(
                data = reviews,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (response.pagination?.hasNextPage == true) page + 1 else null,
            )
        } catch (e: HttpException) {
            LoadResult.Error(e)
        } catch (e: IOException) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, AnimeReview>): Int? =
        state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
}
