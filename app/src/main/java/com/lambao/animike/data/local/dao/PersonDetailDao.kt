package com.lambao.animike.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lambao.animike.data.local.entity.CachedPersonDetailEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDetailDao {
    @Query("SELECT * FROM cached_person_detail WHERE personId = :personId")
    fun observe(personId: Int): Flow<CachedPersonDetailEntity?>

    @Query("SELECT fetchedAt FROM cached_person_detail WHERE personId = :personId")
    suspend fun getFetchedAt(personId: Int): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CachedPersonDetailEntity)
}
