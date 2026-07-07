package com.lambao.animike.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.lambao.animike.data.local.entity.CachedAnimeStaffMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimeStaffDao {
    @Query("SELECT * FROM cached_anime_staff_member WHERE malId = :malId ORDER BY position")
    fun observe(malId: Int): Flow<List<CachedAnimeStaffMemberEntity>>

    // Gate TTL riêng (KHÁC 2 DAO của PersonDetail) — /anime/{id}/staff là API
    // call ĐỘC LẬP với AnimeDetailRepository's các refresh khác, cần sentinel
    // row khi rỗng thật để MIN(fetchedAt) không trả null mãi mãi (xem
    // refreshStaff ở AnimeDetailRepository).
    @Query("SELECT MIN(fetchedAt) FROM cached_anime_staff_member WHERE malId = :malId")
    suspend fun getFetchedAt(malId: Int): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CachedAnimeStaffMemberEntity>)

    @Query("DELETE FROM cached_anime_staff_member WHERE malId = :malId")
    suspend fun clear(malId: Int)

    @Transaction
    suspend fun replace(malId: Int, items: List<CachedAnimeStaffMemberEntity>) {
        clear(malId)
        insertAll(items)
    }
}
