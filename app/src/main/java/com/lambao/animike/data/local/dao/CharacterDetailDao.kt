package com.lambao.animike.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lambao.animike.data.local.entity.CachedCharacterDetailEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDetailDao {
    @Query("SELECT * FROM cached_character_detail WHERE characterId = :characterId")
    fun observe(characterId: Int): Flow<CachedCharacterDetailEntity?>

    @Query("SELECT fetchedAt FROM cached_character_detail WHERE characterId = :characterId")
    suspend fun getFetchedAt(characterId: Int): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CachedCharacterDetailEntity)
}
