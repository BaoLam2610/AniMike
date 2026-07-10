package com.lambao.animike.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.lambao.animike.data.remote.JikanApi
import com.lambao.animike.domain.mapper.toDomain
import com.lambao.animike.domain.model.TopCharacter
import retrofit2.HttpException
import java.io.IOException

/**
 * MVP5 "Top nhân vật" — màn "Xem tất cả" (/top/characters). Pagination CHUẨN
 * (25/trang, 3254 trang). Khử trùng malId XUYÊN trang (seenIds) như
 * CommunityRecommendationsPagingSource — tránh "key already used" nếu Jikan
 * trả trùng giữa 2 trang. Chỉ bắt HttpException/IOException.
 */
class TopCharactersPagingSource(
    private val api: JikanApi,
) : PagingSource<Int, TopCharacter>() {

    // field instance an toàn: Pager tạo PagingSource mới mỗi lần refresh.
    private val seenIds = mutableSetOf<Int>()

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TopCharacter> {
        val page = params.key ?: 1
        return try {
            val response = api.getTopCharacters(page)
            val characters = response.data
                .map { it.toDomain() }
                .filter { seenIds.add(it.malId) }
            LoadResult.Page(
                data = characters,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (response.pagination?.hasNextPage == true) page + 1 else null,
            )
        } catch (e: HttpException) {
            LoadResult.Error(e)
        } catch (e: IOException) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, TopCharacter>): Int? =
        state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
}
