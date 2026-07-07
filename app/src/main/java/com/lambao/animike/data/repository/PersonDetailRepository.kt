package com.lambao.animike.data.repository

import androidx.room.withTransaction
import com.lambao.animike.data.local.AppDatabase
import com.lambao.animike.data.local.CacheTtl
import com.lambao.animike.data.local.dao.PersonDetailDao
import com.lambao.animike.data.local.dao.PersonStaffCreditDao
import com.lambao.animike.data.local.dao.PersonVoiceRoleDao
import com.lambao.animike.data.local.isExpired
import com.lambao.animike.data.remote.JikanApi
import com.lambao.animike.domain.mapper.toDomain
import com.lambao.animike.domain.mapper.toEntity
import com.lambao.animike.domain.mapper.toStaffCredits
import com.lambao.animike.domain.model.PersonDetail
import com.lambao.animike.domain.model.PersonStaffCredit
import com.lambao.animike.domain.model.PersonVoiceRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

// MVP5 People/Seiyuu Detail — aggregate mới (khoá theo personMalId, KHÁC
// malId của anime lẫn characterId) nên tách repository riêng, cùng lý do
// CharacterDetailRepository.
interface PersonDetailRepository {
    fun observePersonDetail(personId: Int): Flow<PersonDetail?>
    fun observeStaffCredits(personId: Int): Flow<List<PersonStaffCredit>>
    fun observeVoiceRoles(personId: Int): Flow<List<PersonVoiceRole>>

    // 1 API call (/people/{id}/full) trả về cả 3 phần trên cùng lúc — cùng
    // kiến trúc CharacterDetailRepository.refreshCharacterDetail (gate TTL
    // dựa trên PersonDetailDao, ghi cả 3 bảng trong 1 transaction để tránh
    // lệch trạng thái nếu bị cancel/exception giữa chừng — bài học từ review
    // Character Detail).
    suspend fun refreshPersonDetail(personId: Int, force: Boolean = false): ApiResult<Unit>
}

class PersonDetailRepositoryImpl @Inject constructor(
    private val api: JikanApi,
    private val database: AppDatabase,
    private val personDetailDao: PersonDetailDao,
    private val staffCreditDao: PersonStaffCreditDao,
    private val voiceRoleDao: PersonVoiceRoleDao,
) : PersonDetailRepository {

    override fun observePersonDetail(personId: Int): Flow<PersonDetail?> =
        personDetailDao.observe(personId).map { it?.toDomain() }

    override fun observeStaffCredits(personId: Int): Flow<List<PersonStaffCredit>> =
        staffCreditDao.observe(personId).map { entities -> entities.map { it.toDomain() } }

    override fun observeVoiceRoles(personId: Int): Flow<List<PersonVoiceRole>> =
        voiceRoleDao.observe(personId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun refreshPersonDetail(personId: Int, force: Boolean): ApiResult<Unit> {
        if (!force) {
            val fetchedAt = personDetailDao.getFetchedAt(personId)
            if (fetchedAt != null && !isExpired(fetchedAt, CacheTtl.PERSON_DETAIL_MS)) {
                return ApiResult.Success(Unit)
            }
        }
        return safeApiCall {
            val dto = api.getPersonFull(personId).data
            val fetchedAt = System.currentTimeMillis()

            // toStaffCredits(): gộp theo anime.malId (1 người có thể giữ NHIỀU
            // vai trò trên CÙNG 1 anime, xem PersonDetailMapper) — KHÔNG dùng
            // distinctBy đơn giản như voiceRoles vì sẽ mất credit thật.
            val staffCredits = dto.anime.toStaffCredits()
            // distinctBy: phòng Jikan trả trùng trong cùng response — PK
            // composite sẽ REPLACE âm thầm và lệch position nếu không khử
            // trùng trước (cùng lý do CharacterDetailRepository). KHÔNG cần
            // sentinel row khi rỗng — TTL gate dùng bảng core.
            val voiceRoles = dto.voices.mapNotNull { it.toDomain() }.distinctBy { it.anime.malId to it.characterMalId }

            database.withTransaction {
                personDetailDao.upsert(dto.toDomain().toEntity(fetchedAt))
                staffCreditDao.replace(
                    personId,
                    staffCredits.mapIndexed { index, item -> item.toEntity(personId, index, fetchedAt) },
                )
                voiceRoleDao.replace(
                    personId,
                    voiceRoles.mapIndexed { index, item -> item.toEntity(personId, index, fetchedAt) },
                )
            }
        }
    }
}
