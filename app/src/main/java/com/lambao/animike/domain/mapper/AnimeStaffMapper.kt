package com.lambao.animike.domain.mapper

import com.lambao.animike.data.remote.dto.StaffEntryDto
import com.lambao.animike.domain.model.AnimeStaffMember

fun StaffEntryDto.toDomain(): AnimeStaffMember? {
    val personRef = person ?: return null
    return AnimeStaffMember(
        personMalId = personRef.malId,
        name = personRef.name ?: "Không rõ tên",
        imageUrl = personRef.images?.jpg?.largeImageUrl ?: personRef.images?.jpg?.imageUrl,
        positions = positions.distinct(),
    )
}
