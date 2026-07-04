package com.lambao.animike.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.lambao.animike.data.remote.JikanApi
import com.lambao.animike.domain.mapper.toDomain
import com.lambao.animike.domain.model.Anime
import com.lambao.animike.domain.model.SearchFilters
import retrofit2.HttpException
import java.io.IOException

/**
 * Chỉ bắt HttpException/IOException — không catch(Exception) rộng, đúng
 * anti-pattern list trong compose-expert/references/paging-mvi-testing.md
 * ("Hides real bugs as recoverable errors").
 */
class AnimeSearchPagingSource(
    private val api: JikanApi,
    private val query: String,
    private val filters: SearchFilters,
) : PagingSource<Int, Anime>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Anime> {
        val page = params.key ?: 1
        return try {
            val response = api.searchAnime(
                // query rỗng -> gửi null để Retrofit bỏ hẳn tham số q= (khác với
                // q= rỗng) — Jikan mới trả danh sách chung theo order_by/sort.
                query = query.ifBlank { null },
                page = page,
                type = filters.type,
                status = filters.status,
                genres = filters.genreIds.takeIf { it.isNotEmpty() }?.joinToString(","),
                orderBy = filters.orderBy,
                sort = filters.sort,
            )
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
