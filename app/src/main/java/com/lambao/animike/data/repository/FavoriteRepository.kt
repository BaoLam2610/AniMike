package com.lambao.animike.data.repository

import android.util.Log
import com.lambao.animike.data.local.dao.FavoriteDao
import com.lambao.animike.domain.mapper.toDomain
import com.lambao.animike.domain.mapper.toFavoriteEntity
import com.lambao.animike.domain.model.Anime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private const val TAG = "FavoriteRepository"

interface FavoriteRepository {
    fun observeFavorites(): Flow<List<Anime>>
    fun observeIsFavorite(malId: Int): Flow<Boolean>
    suspend fun toggleFavorite(anime: Anime)
}

class FavoriteRepositoryImpl @Inject constructor(
    private val dao: FavoriteDao,
) : FavoriteRepository {

    override fun observeFavorites(): Flow<List<Anime>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeIsFavorite(malId: Int): Flow<Boolean> = dao.observeIsFavorite(malId)

    override suspend fun toggleFavorite(anime: Anime) {
        try {
            // dao.toggle() gộp đọc+ghi trong 1 @Transaction — tránh race khi
            // double-tap (2 lệnh gọi riêng biệt có thể cùng đọc trạng thái cũ).
            dao.toggle(anime.toFavoriteEntity(System.currentTimeMillis()))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Local-only, không nghiêm trọng — icon trái tim không đổi, user bấm lại được.
            Log.w(TAG, "Không toggle được favorite cho malId=${anime.malId}", e)
        }
    }
}
