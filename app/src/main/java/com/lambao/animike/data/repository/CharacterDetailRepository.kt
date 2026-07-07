package com.lambao.animike.data.repository

import androidx.room.withTransaction
import com.lambao.animike.data.local.AppDatabase
import com.lambao.animike.data.local.CacheTtl
import com.lambao.animike.data.local.dao.CharacterAnimeAppearanceDao
import com.lambao.animike.data.local.dao.CharacterDetailDao
import com.lambao.animike.data.local.dao.CharacterVoiceActorDao
import com.lambao.animike.data.local.isExpired
import com.lambao.animike.data.remote.JikanApi
import com.lambao.animike.domain.mapper.toDomain
import com.lambao.animike.domain.mapper.toEntity
import com.lambao.animike.domain.model.CharacterAnimeAppearance
import com.lambao.animike.domain.model.CharacterDetail
import com.lambao.animike.domain.model.CharacterVoiceActor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

// MVP5 Character Detail — aggregate mới (khoá theo characterId, KHÁC malId
// của anime) nên tách repository riêng thay vì nhét vào AnimeDetailRepository
// (đã có 10 cặp observe/refresh cho aggregate "anime", thêm 1 aggregate khác
// vào đó sẽ quá tải file).
interface CharacterDetailRepository {
    fun observeCharacterDetail(characterId: Int): Flow<CharacterDetail?>
    fun observeAnimeAppearances(characterId: Int): Flow<List<CharacterAnimeAppearance>>
    fun observeVoiceActors(characterId: Int): Flow<List<CharacterVoiceActor>>

    // 1 API call (/characters/{id}/full) trả về cả 3 phần trên cùng lúc nên
    // chỉ cần 1 hàm refresh, gate TTL dựa trên CharacterDetailDao (bảng core,
    // luôn có đúng 1 row) — 2 bảng danh sách không cần getFetchedAt riêng.
    suspend fun refreshCharacterDetail(characterId: Int, force: Boolean = false): ApiResult<Unit>
}

class CharacterDetailRepositoryImpl @Inject constructor(
    private val api: JikanApi,
    private val database: AppDatabase,
    private val characterDetailDao: CharacterDetailDao,
    private val animeAppearanceDao: CharacterAnimeAppearanceDao,
    private val voiceActorDao: CharacterVoiceActorDao,
) : CharacterDetailRepository {

    override fun observeCharacterDetail(characterId: Int): Flow<CharacterDetail?> =
        characterDetailDao.observe(characterId).map { it?.toDomain() }

    override fun observeAnimeAppearances(characterId: Int): Flow<List<CharacterAnimeAppearance>> =
        animeAppearanceDao.observe(characterId).map { entities -> entities.map { it.toDomain() } }

    override fun observeVoiceActors(characterId: Int): Flow<List<CharacterVoiceActor>> =
        voiceActorDao.observe(characterId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun refreshCharacterDetail(characterId: Int, force: Boolean): ApiResult<Unit> {
        if (!force) {
            val fetchedAt = characterDetailDao.getFetchedAt(characterId)
            if (fetchedAt != null && !isExpired(fetchedAt, CacheTtl.CHARACTER_DETAIL_MS)) {
                return ApiResult.Success(Unit)
            }
        }
        return safeApiCall {
            val dto = api.getCharacterFull(characterId).data
            val fetchedAt = System.currentTimeMillis()

            // distinctBy: phòng Jikan trả trùng trong cùng response (cùng lý
            // do Characters/Recommendations ở AnimeDetailRepository) — PK
            // composite sẽ REPLACE âm thầm và lệch position nếu không khử
            // trùng trước. KHÔNG cần sentinel row khi rỗng — TTL gate ở trên
            // dùng bảng core (luôn có đúng 1 row), 2 bảng này rỗng thật vẫn
            // đọc đúng qua Flow (không có getFetchedAt riêng để bị kẹt null).
            val appearances = dto.anime.mapNotNull { it.toDomain() }.distinctBy { it.anime.malId }
            val voiceActors = dto.voices.mapNotNull { it.toDomain() }.distinctBy { it.personMalId }

            // withTransaction: 3 lệnh ghi PHẢI atomic — TTL gate ở trên chỉ
            // check bảng core (characterDetailDao.getFetchedAt), nên nếu core
            // commit xong nhưng 2 bảng danh sách chưa kịp ghi (coroutine bị
            // cancel giữa chừng, hoặc 1 trong 2 replace() ném exception), lần
            // refresh tiếp theo sẽ thấy core "còn tươi" và bỏ qua gọi lại API
            // mãi mãi — 2 section sẽ trống rỗng sai suốt TTL 7 ngày mà user
            // không có cách tự sửa (phát hiện qua compose-reviewer, sửa ngay).
            database.withTransaction {
                characterDetailDao.upsert(dto.toDomain().toEntity(fetchedAt))
                animeAppearanceDao.replace(
                    characterId,
                    appearances.mapIndexed { index, item -> item.toEntity(characterId, index, fetchedAt) },
                )
                voiceActorDao.replace(
                    characterId,
                    voiceActors.mapIndexed { index, item -> item.toEntity(characterId, index, fetchedAt) },
                )
            }
        }
    }
}
