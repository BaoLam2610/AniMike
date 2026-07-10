package com.lambao.animike.data.repository

import androidx.paging.PagingSource
import com.lambao.animike.data.local.CacheTtl
import com.lambao.animike.data.local.dao.StudioDetailDao
import com.lambao.animike.data.local.isExpired
import com.lambao.animike.data.remote.JikanApi
import com.lambao.animike.domain.mapper.toDomain
import com.lambao.animike.domain.mapper.toEntity
import com.lambao.animike.domain.model.Anime
import com.lambao.animike.domain.model.StudioDetail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

// MVP5 Studio Detail — aggregate mới (khoá theo producerId). CHỈ 1 bảng core
// (không có 2 bảng list như Character/Person Detail) vì danh sách anime của
// studio đi qua Paging 3 (searchAnime?producers=), KHÔNG cache Room → không
// cần transaction đa-bảng như 2 màn kia.
interface StudioDetailRepository {
    fun observeStudioDetail(studioId: Int): Flow<StudioDetail?>
    suspend fun refreshStudioDetail(studioId: Int, force: Boolean = false): ApiResult<Unit>

    // Danh sách anime studio sản xuất — Paging 3, không cache (list dài tới
    // hàng trăm item, tái dùng searchAnime(producers=id)).
    fun studioAnimePagingSource(studioId: Int): PagingSource<Int, Anime>
}

class StudioDetailRepositoryImpl @Inject constructor(
    private val api: JikanApi,
    private val studioDetailDao: StudioDetailDao,
) : StudioDetailRepository {

    override fun observeStudioDetail(studioId: Int): Flow<StudioDetail?> =
        studioDetailDao.observe(studioId).map { it?.toDomain() }

    override suspend fun refreshStudioDetail(studioId: Int, force: Boolean): ApiResult<Unit> {
        if (!force) {
            val fetchedAt = studioDetailDao.getFetchedAt(studioId)
            if (fetchedAt != null && !isExpired(fetchedAt, CacheTtl.STUDIO_DETAIL_MS)) {
                return ApiResult.Success(Unit)
            }
        }
        return safeApiCall {
            val studio = api.getProducerFull(studioId).data.toDomain()
            studioDetailDao.upsert(studio.toEntity(System.currentTimeMillis()))
        }
    }

    override fun studioAnimePagingSource(studioId: Int): PagingSource<Int, Anime> =
        StudioAnimePagingSource(api, studioId)
}
