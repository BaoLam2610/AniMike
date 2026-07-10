package com.lambao.animike.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.lambao.animike.data.remote.JikanApi
import com.lambao.animike.domain.mapper.toDomain
import com.lambao.animike.domain.model.Anime
import retrofit2.HttpException
import java.io.IOException

/**
 * MVP5 Studio Detail — danh sách anime của 1 studio, TÁI DÙNG endpoint search
 * `searchAnime` chỉ thêm `producers={id}` (verify: pagination thật, xem
 * references/mvp5-characters-people-studio.md). Cùng khuôn AnimeSearchPagingSource
 * (chỉ bắt HttpException/IOException, không catch rộng).
 */
class StudioAnimePagingSource(
    private val api: JikanApi,
    private val producerId: Int,
) : PagingSource<Int, Anime>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Anime> {
        val page = params.key ?: 1
        return try {
            val response = api.searchAnime(producers = producerId, page = page)
            val anime = response.data.map { it.toDomain() }.distinctBy { it.malId }
            LoadResult.Page(
                data = anime,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (response.pagination?.hasNextPage == true) page + 1 else null,
            )
        } catch (e: HttpException) {
            LoadResult.Error(e)
        } catch (e: IOException) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Anime>): Int? =
        state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
}
