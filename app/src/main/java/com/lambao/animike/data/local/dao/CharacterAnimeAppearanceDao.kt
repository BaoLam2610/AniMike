package com.lambao.animike.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.lambao.animike.data.local.entity.CachedCharacterAnimeAppearanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterAnimeAppearanceDao {
    @Query("SELECT * FROM cached_character_anime_appearance WHERE characterId = :characterId ORDER BY position")
    fun observe(characterId: Int): Flow<List<CachedCharacterAnimeAppearanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CachedCharacterAnimeAppearanceEntity>)

    @Query("DELETE FROM cached_character_anime_appearance WHERE characterId = :characterId")
    suspend fun clear(characterId: Int)

    // Không cần getFetchedAt riêng — TTL gate dùng chung CharacterDetailDao
    // (cả 2 bảng này luôn ghi cùng lúc trong 1 lần gọi /characters/{id}/full).
    @Transaction
    suspend fun replace(characterId: Int, items: List<CachedCharacterAnimeAppearanceEntity>) {
        clear(characterId)
        insertAll(items)
    }
}
