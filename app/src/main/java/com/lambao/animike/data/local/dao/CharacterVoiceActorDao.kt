package com.lambao.animike.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.lambao.animike.data.local.entity.CachedCharacterVoiceActorEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterVoiceActorDao {
    @Query("SELECT * FROM cached_character_voice_actor WHERE characterId = :characterId ORDER BY position")
    fun observe(characterId: Int): Flow<List<CachedCharacterVoiceActorEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CachedCharacterVoiceActorEntity>)

    @Query("DELETE FROM cached_character_voice_actor WHERE characterId = :characterId")
    suspend fun clear(characterId: Int)

    // Không cần getFetchedAt riêng — cùng lý do CharacterAnimeAppearanceDao.
    @Transaction
    suspend fun replace(characterId: Int, items: List<CachedCharacterVoiceActorEntity>) {
        clear(characterId)
        insertAll(items)
    }
}
