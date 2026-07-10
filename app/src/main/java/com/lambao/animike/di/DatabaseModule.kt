package com.lambao.animike.di

import android.content.Context
import androidx.room.Room
import com.lambao.animike.data.local.AppDatabase
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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val DATABASE_NAME = "animike.db"

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
            // Chưa có user thật, dữ liệu toàn là cache/vài favorite test — mất
            // khi đổi schema là chấp nhận được. Cần thêm Migration thật khi
            // release (xem docs/ROADMAP.md).
            .fallbackToDestructiveMigration(true)
            .build()

    @Provides
    fun provideAnimeListDao(database: AppDatabase): AnimeListDao = database.animeListDao()

    @Provides
    fun provideAnimeDetailDao(database: AppDatabase): AnimeDetailDao = database.animeDetailDao()

    @Provides
    fun provideGenreDao(database: AppDatabase): GenreDao = database.genreDao()

    @Provides
    fun provideFavoriteDao(database: AppDatabase): FavoriteDao = database.favoriteDao()

    @Provides
    fun provideSeasonYearDao(database: AppDatabase): SeasonYearDao = database.seasonYearDao()

    @Provides
    fun providePictureDao(database: AppDatabase): PictureDao = database.pictureDao()

    @Provides
    fun provideReviewPreviewDao(database: AppDatabase): ReviewPreviewDao = database.reviewPreviewDao()

    @Provides
    fun provideNewEpisodeDao(database: AppDatabase): NewEpisodeDao = database.newEpisodeDao()

    @Provides
    fun provideCharacterDao(database: AppDatabase): CharacterDao = database.characterDao()

    @Provides
    fun provideCommunityRecommendationDao(database: AppDatabase): CommunityRecommendationDao =
        database.communityRecommendationDao()

    @Provides
    fun provideAnimeStatisticsDao(database: AppDatabase): AnimeStatisticsDao = database.animeStatisticsDao()

    @Provides
    fun provideAnimeThemesDao(database: AppDatabase): AnimeThemesDao = database.animeThemesDao()

    @Provides
    fun provideStreamingLinkDao(database: AppDatabase): StreamingLinkDao = database.streamingLinkDao()

    @Provides
    fun provideAnimeVideoDao(database: AppDatabase): AnimeVideoDao = database.animeVideoDao()

    @Provides
    fun provideCharacterDetailDao(database: AppDatabase): CharacterDetailDao = database.characterDetailDao()

    @Provides
    fun provideCharacterAnimeAppearanceDao(database: AppDatabase): CharacterAnimeAppearanceDao =
        database.characterAnimeAppearanceDao()

    @Provides
    fun provideCharacterVoiceActorDao(database: AppDatabase): CharacterVoiceActorDao =
        database.characterVoiceActorDao()

    @Provides
    fun providePersonDetailDao(database: AppDatabase): PersonDetailDao = database.personDetailDao()

    @Provides
    fun providePersonStaffCreditDao(database: AppDatabase): PersonStaffCreditDao = database.personStaffCreditDao()

    @Provides
    fun providePersonVoiceRoleDao(database: AppDatabase): PersonVoiceRoleDao = database.personVoiceRoleDao()

    @Provides
    fun provideAnimeStaffDao(database: AppDatabase): AnimeStaffDao = database.animeStaffDao()

    @Provides
    fun provideStudioDetailDao(database: AppDatabase): StudioDetailDao = database.studioDetailDao()

    @Provides
    fun provideTopCharacterDao(database: AppDatabase): TopCharacterDao = database.topCharacterDao()
}
