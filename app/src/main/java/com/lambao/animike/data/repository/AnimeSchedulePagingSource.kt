package com.lambao.animike.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.lambao.animike.data.remote.JikanApi
import com.lambao.animike.domain.mapper.toScheduledAnime
import com.lambao.animike.domain.model.ScheduledAnime
import retrofit2.HttpException
import java.io.IOException

/**
 * Chỉ bắt HttpException/IOException — không catch(Exception) rộng, đúng
 * anti-pattern list trong compose-expert/references/paging-mvi-testing.md.
 */
class AnimeSchedulePagingSource(
    private val api: JikanApi,
    private val day: String,
) : PagingSource<Int, ScheduledAnime>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ScheduledAnime> {
        val page = params.key ?: 1
        return try {
            val response = api.getSchedules(day, page)
            val anime = response.data.map { it.toScheduledAnime() }.distinctBy { it.malId }
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

    override fun getRefreshKey(state: PagingState<Int, ScheduledAnime>): Int? =
        state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
}
