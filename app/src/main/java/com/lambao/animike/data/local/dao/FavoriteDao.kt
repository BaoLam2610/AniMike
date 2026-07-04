package com.lambao.animike.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.lambao.animike.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorite ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite WHERE malId = :malId)")
    fun observeIsFavorite(malId: Int): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite WHERE malId = :malId)")
    suspend fun isFavoriteOnce(malId: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FavoriteEntity)

    @Query("DELETE FROM favorite WHERE malId = :malId")
    suspend fun delete(malId: Int)

    // Gộp đọc + ghi vào 1 transaction — tránh TOCTOU khi double-tap: 2 lệnh gọi
    // riêng biệt (đọc rồi ghi) có thể cùng đọc trạng thái cũ và cùng ghi y hệt
    // nhau (VD cả 2 cùng insert), khiến 2 lần tap chỉ tính là 1 lần toggle.
    @Transaction
    suspend fun toggle(entity: FavoriteEntity) {
        if (isFavoriteOnce(entity.malId)) {
            delete(entity.malId)
        } else {
            insert(entity)
        }
    }
}
