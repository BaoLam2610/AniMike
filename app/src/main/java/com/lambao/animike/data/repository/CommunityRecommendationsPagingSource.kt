package com.lambao.animike.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.lambao.animike.data.remote.JikanApi
import com.lambao.animike.domain.mapper.toDomain
import com.lambao.animike.domain.model.CommunityRecommendation
import retrofit2.HttpException
import java.io.IOException

/**
 * Paging cho màn "Xem tất cả" của "Đề xuất cộng đồng" (/recommendations/anime)
 * — endpoint này phân trang THẬT (100 item/trang, đã verify qua curl), khác
 * hẳn /watch/episodes*. Chỉ bắt HttpException/IOException — không
 * catch(Exception) rộng (quy ước compose-expert/references/paging-mvi-testing.md).
 */
class CommunityRecommendationsPagingSource(
    private val api: JikanApi,
) : PagingSource<Int, CommunityRecommendation>() {

    // Khử trùng id XUYÊN trang (cùng lý do với AnimeListPagingSource/
    // AnimeReviewsPagingSource) — field instance an toàn vì Pager tạo
    // PagingSource mới mỗi lần refresh.
    private val seenIds = mutableSetOf<String>()

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, CommunityRecommendation> {
        val page = params.key ?: 1
        return try {
            val response = api.getCommunityRecommendations(page)
            val recommendations = response.data
                .mapNotNull { it.toDomain() }
                .filter { seenIds.add(it.id) }
            LoadResult.Page(
                data = recommendations,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (response.pagination?.hasNextPage == true) page + 1 else null,
            )
        } catch (e: HttpException) {
            LoadResult.Error(e)
        } catch (e: IOException) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, CommunityRecommendation>): Int? =
        state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
}
