package com.lambao.animike.di

import android.content.Context
import androidx.room.Room
import com.lambao.animike.data.local.AppDatabase
import com.lambao.animike.data.local.dao.AnimeDetailDao
import com.lambao.animike.data.local.dao.AnimeListDao
import com.lambao.animike.data.local.dao.FavoriteDao
import com.lambao.animike.data.local.dao.GenreDao
import com.lambao.animike.data.local.dao.SeasonYearDao
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
}
