package com.lambao.animike.domain.mapper

import com.lambao.animike.data.remote.dto.PersonFullDto
import com.lambao.animike.data.remote.dto.PersonStaffRefDto
import com.lambao.animike.data.remote.dto.PersonVoiceRefDto
import com.lambao.animike.domain.model.PersonDetail
import com.lambao.animike.domain.model.PersonStaffCredit
import com.lambao.animike.domain.model.PersonVoiceRole

fun PersonFullDto.toDomain(): PersonDetail = PersonDetail(
    malId = malId,
    name = name ?: "Không rõ tên",
    givenName = givenName,
    familyName = familyName,
    imageUrl = images?.jpg?.largeImageUrl ?: images?.jpg?.imageUrl,
    // distinct(): cùng lý do nicknames ở CharacterDetailMapper — dùng làm
    // LazyRow key nên trùng phần tử sẽ crash.
    alternateNames = alternateNames.distinct(),
    birthday = birthday,
    favorites = favorites ?: 0,
    about = about,
)

// List-level (KHÔNG phải per-item toDomain()) — Jikan trả anime[] dạng PHẲNG
// {position, anime}, 1 người có thể giữ NHIỀU vai trò trên CÙNG 1 anime (2
// entry riêng cùng anime.mal_id, khác position). groupBy theo anime.malId rồi
// gộp positions — bản đầu dùng distinctBy{it.anime.malId} đơn giản đã ÂM THẦM
// LÀM MẤT credit thật trong trường hợp này (phát hiện qua review, sửa).
fun List<PersonStaffRefDto>.toStaffCredits(): List<PersonStaffCredit> =
    mapNotNull { ref -> val animeRef = ref.anime ?: return@mapNotNull null; (ref.position ?: "") to animeRef.toDomain() }
        .groupBy { it.second.malId }
        .map { (_, entries) ->
            PersonStaffCredit(
                positions = entries.map { it.first }.filter { it.isNotEmpty() }.distinct(),
                anime = entries.first().second,
            )
        }

fun PersonVoiceRefDto.toDomain(): PersonVoiceRole? {
    val animeRef = anime ?: return null
    val characterRef = character ?: return null
    return PersonVoiceRole(
        role = role ?: "",
        anime = animeRef.toDomain(),
        characterMalId = characterRef.malId,
        characterName = characterRef.name ?: "Không rõ tên",
        characterImageUrl = characterRef.images?.jpg?.largeImageUrl ?: characterRef.images?.jpg?.imageUrl,
    )
}
