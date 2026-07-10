package com.lambao.animike.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.lambao.animike.data.local.dao.AnimeDetailDao
import com.lambao.animike.data.local.dao.AnimeListDao
import com.lambao.animike.data.local.dao.AnimeStaffDao
import com.lambao.animike.data.local.dao.AnimeStatisticsDao
import com.lambao.animike.data.local.dao.AnimeThemesDao
import com.lambao.animike.data.local.dao.AnimeVideoDao
import com.lambao.animike.data.local.dao.CharacterAnimeAppearanceDao
import com.lambao.animike.data.local.dao.CharacterDao
import com.lambao.animike.data.local.dao.CharacterDetailDao
import com.lambao.animike.data.local.dao.CharacterVoiceActorDao
import com.lambao.animike.data.local.dao.CommunityRecommendationDao
import com.lambao.animike.data.local.dao.FavoriteDao
import com.lambao.animike.data.local.dao.GenreDao
import com.lambao.animike.data.local.dao.NewEpisodeDao
import com.lambao.animike.data.local.dao.PersonDetailDao
import com.lambao.animike.data.local.dao.PersonStaffCreditDao
import com.lambao.animike.data.local.dao.PersonVoiceRoleDao
import com.lambao.animike.data.local.dao.PictureDao
import com.lambao.animike.data.local.dao.ReviewPreviewDao
import com.lambao.animike.data.local.dao.SeasonYearDao
import com.lambao.animike.data.local.dao.StreamingLinkDao
import com.lambao.animike.data.local.dao.StudioDetailDao
import com.lambao.animike.data.local.dao.TopCharacterDao
import com.lambao.animike.data.local.entity.CachedAnimeDetailEntity
import com.lambao.animike.data.local.entity.CachedAnimeListEntity
import com.lambao.animike.data.local.entity.CachedAnimeStaffMemberEntity
import com.lambao.animike.data.local.entity.CachedAnimeStatisticsEntity
import com.lambao.animike.data.local.entity.CachedAnimeThemesEntity
import com.lambao.animike.data.local.entity.CachedAnimeVideoEntity
import com.lambao.animike.data.local.entity.CachedCharacterAnimeAppearanceEntity
import com.lambao.animike.data.local.entity.CachedCharacterDetailEntity
import com.lambao.animike.data.local.entity.CachedCharacterEntity
import com.lambao.animike.data.local.entity.CachedCharacterVoiceActorEntity
import com.lambao.animike.data.local.entity.CachedCommunityRecommendationEntity
import com.lambao.animike.data.local.entity.CachedGenreEntity
import com.lambao.animike.data.local.entity.CachedNewEpisodeEntity
import com.lambao.animike.data.local.entity.CachedPersonDetailEntity
import com.lambao.animike.data.local.entity.CachedPersonStaffCreditEntity
import com.lambao.animike.data.local.entity.CachedPersonVoiceRoleEntity
import com.lambao.animike.data.local.entity.CachedPictureEntity
import com.lambao.animike.data.local.entity.CachedReviewPreviewEntity
import com.lambao.animike.data.local.entity.CachedSeasonYearEntity
import com.lambao.animike.data.local.entity.CachedStreamingLinkEntity
import com.lambao.animike.data.local.entity.CachedStudioDetailEntity
import com.lambao.animike.data.local.entity.CachedTopCharacterEntity
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
        CachedStreamingLinkEntity::class,
        CachedAnimeVideoEntity::class,
        CachedCharacterDetailEntity::class,
        CachedCharacterAnimeAppearanceEntity::class,
        CachedCharacterVoiceActorEntity::class,
        CachedPersonDetailEntity::class,
        CachedPersonStaffCreditEntity::class,
        CachedPersonVoiceRoleEntity::class,
        CachedAnimeStaffMemberEntity::class,
        CachedStudioDetailEntity::class,
        CachedTopCharacterEntity::class,
    ],
    version = 16,
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
    abstract fun streamingLinkDao(): StreamingLinkDao
    abstract fun animeVideoDao(): AnimeVideoDao
    abstract fun characterDetailDao(): CharacterDetailDao
    abstract fun characterAnimeAppearanceDao(): CharacterAnimeAppearanceDao
    abstract fun characterVoiceActorDao(): CharacterVoiceActorDao
    abstract fun personDetailDao(): PersonDetailDao
    abstract fun personStaffCreditDao(): PersonStaffCreditDao
    abstract fun personVoiceRoleDao(): PersonVoiceRoleDao
    abstract fun animeStaffDao(): AnimeStaffDao
    abstract fun studioDetailDao(): StudioDetailDao
    abstract fun topCharacterDao(): TopCharacterDao
}
