package com.lambao.animike.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.lambao.animike.data.local.entity.CachedPersonStaffCreditEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonStaffCreditDao {
    @Query("SELECT * FROM cached_person_staff_credit WHERE personId = :personId ORDER BY position")
    fun observe(personId: Int): Flow<List<CachedPersonStaffCreditEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CachedPersonStaffCreditEntity>)

    @Query("DELETE FROM cached_person_staff_credit WHERE personId = :personId")
    suspend fun clear(personId: Int)

    // Không cần getFetchedAt riêng — TTL gate dùng chung PersonDetailDao (cả
    // 2 bảng này luôn ghi cùng lúc trong 1 lần gọi /people/{id}/full).
    @Transaction
    suspend fun replace(personId: Int, items: List<CachedPersonStaffCreditEntity>) {
        clear(personId)
        insertAll(items)
    }
}
