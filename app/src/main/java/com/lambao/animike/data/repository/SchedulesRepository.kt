package com.lambao.animike.data.repository

import androidx.paging.PagingSource
import com.lambao.animike.data.remote.JikanApi
import com.lambao.animike.domain.model.ScheduledAnime
import javax.inject.Inject

// Không cache Room: danh sách thứ trong tuần cố định (không cần fetch/lưu như
// SeasonArchiveRepository.observeSeasonsList), còn nội dung từng ngày lấy
// trực tiếp qua PagingSource giống Search (jikan-api SKILL.md: Search KHÔNG cache).
interface SchedulesRepository {
    fun schedulePagingSource(day: String): PagingSource<Int, ScheduledAnime>
}

class SchedulesRepositoryImpl @Inject constructor(
    private val api: JikanApi,
) : SchedulesRepository {

    override fun schedulePagingSource(day: String): PagingSource<Int, ScheduledAnime> =
        AnimeSchedulePagingSource(api, day)
}
