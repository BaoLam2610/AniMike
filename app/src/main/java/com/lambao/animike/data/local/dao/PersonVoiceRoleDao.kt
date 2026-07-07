package com.lambao.animike.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.lambao.animike.data.local.entity.CachedPersonVoiceRoleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonVoiceRoleDao {
    @Query("SELECT * FROM cached_person_voice_role WHERE personId = :personId ORDER BY position")
    fun observe(personId: Int): Flow<List<CachedPersonVoiceRoleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CachedPersonVoiceRoleEntity>)

    @Query("DELETE FROM cached_person_voice_role WHERE personId = :personId")
    suspend fun clear(personId: Int)

    // Không cần getFetchedAt riêng — cùng lý do PersonStaffCreditDao.
    @Transaction
    suspend fun replace(personId: Int, items: List<CachedPersonVoiceRoleEntity>) {
        clear(personId)
        insertAll(items)
    }
}
