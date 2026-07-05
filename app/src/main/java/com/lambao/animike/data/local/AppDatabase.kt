package com.lambao.animike.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.lambao.animike.data.local.dao.AnimeDetailDao
import com.lambao.animike.data.local.dao.AnimeListDao
import com.lambao.animike.data.local.dao.AnimeStatisticsDao
import com.lambao.animike.data.local.dao.AnimeThemesDao
import com.lambao.animike.data.local.dao.CharacterDao
import com.lambao.animike.data.local.dao.CommunityRecommendationDao
import com.lambao.animike.data.local.dao.FavoriteDao
import com.lambao.animike.data.local.dao.GenreDao
import com.lambao.animike.data.local.dao.NewEpisodeDao
import com.lambao.animike.data.local.dao.PictureDao
import com.lambao.animike.data.local.dao.ReviewPreviewDao
import com.lambao.animike.data.local.dao.SeasonYearDao
import com.lambao.animike.data.local.entity.CachedAnimeDetailEntity
import com.lambao.animike.data.local.entity.CachedAnimeListEntity
import com.lambao.animike.data.local.entity.CachedAnimeStatisticsEntity
import com.lambao.animike.data.local.entity.CachedAnimeThemesEntity
import com.lambao.animike.data.local.entity.CachedCharacterEntity
import com.lambao.animike.data.local.entity.CachedCommunityRecommendationEntity
import com.lambao.animike.data.local.entity.CachedGenreEntity
import com.lambao.animike.data.local.entity.CachedNewEpisodeEntity
import com.lambao.animike.data.local.entity.CachedPictureEntity
import com.lambao.animike.data.local.entity.CachedReviewPreviewEntity
import com.lambao.animike.data.local.entity.CachedSeasonYearEntity
import com.lambao.animike.data.local.entity.FavoriteEntity

@Database(
    entities = [
        CachedAnimeListEntity::class,
        CachedAnimeDetailEntity::class,
        CachedGenreEntity::class,
        FavoriteEntity::class,
        CachedSeasonYearEntity::class,
        CachedPictureEntity::class,
        CachedReviewPreviewEntity::class,
        CachedNewEpisodeEntity::class,
        CachedCharacterEntity::class,
        CachedCommunityRecommendationEntity::class,
        CachedAnimeStatisticsEntity::class,
        CachedAnimeThemesEntity::class,
    ],
    version = 9,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun animeListDao(): AnimeListDao
    abstract fun animeDetailDao(): AnimeDetailDao
    abstract fun genreDao(): GenreDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun seasonYearDao(): SeasonYearDao
    abstract fun pictureDao(): PictureDao
    abstract fun reviewPreviewDao(): ReviewPreviewDao
    abstract fun newEpisodeDao(): NewEpisodeDao
    abstract fun characterDao(): CharacterDao
    abstract fun communityRecommendationDao(): CommunityRecommendationDao
    abstract fun animeStatisticsDao(): AnimeStatisticsDao
    abstract fun animeThemesDao(): AnimeThemesDao
}
