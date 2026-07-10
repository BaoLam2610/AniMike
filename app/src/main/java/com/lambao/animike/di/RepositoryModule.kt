package com.lambao.animike.di

import com.lambao.animike.data.repository.AnimeDetailRepository
import com.lambao.animike.data.repository.AnimeDetailRepositoryImpl
import com.lambao.animike.data.repository.AnimeRepository
import com.lambao.animike.data.repository.AnimeRepositoryImpl
import com.lambao.animike.data.repository.CharacterDetailRepository
import com.lambao.animike.data.repository.CharacterDetailRepositoryImpl
import com.lambao.animike.data.repository.FavoriteRepository
import com.lambao.animike.data.repository.FavoriteRepositoryImpl
import com.lambao.animike.data.repository.PersonDetailRepository
import com.lambao.animike.data.repository.PersonDetailRepositoryImpl
import com.lambao.animike.data.repository.SchedulesRepository
import com.lambao.animike.data.repository.SchedulesRepositoryImpl
import com.lambao.animike.data.repository.SearchRepository
import com.lambao.animike.data.repository.SearchRepositoryImpl
import com.lambao.animike.data.repository.SeasonArchiveRepository
import com.lambao.animike.data.repository.SeasonArchiveRepositoryImpl
import com.lambao.animike.data.repository.StudioDetailRepository
import com.lambao.animike.data.repository.StudioDetailRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAnimeRepository(impl: AnimeRepositoryImpl): AnimeRepository

    @Binds
    @Singleton
    abstract fun bindAnimeDetailRepository(impl: AnimeDetailRepositoryImpl): AnimeDetailRepository

    @Binds
    @Singleton
    abstract fun bindSearchRepository(impl: SearchRepositoryImpl): SearchRepository

    @Binds
    @Singleton
    abstract fun bindFavoriteRepository(impl: FavoriteRepositoryImpl): FavoriteRepository

    @Binds
    @Singleton
    abstract fun bindSeasonArchiveRepository(impl: SeasonArchiveRepositoryImpl): SeasonArchiveRepository

    @Binds
    @Singleton
    abstract fun bindSchedulesRepository(impl: SchedulesRepositoryImpl): SchedulesRepository

    @Binds
    @Singleton
    abstract fun bindCharacterDetailRepository(impl: CharacterDetailRepositoryImpl): CharacterDetailRepository

    @Binds
    @Singleton
    abstract fun bindPersonDetailRepository(impl: PersonDetailRepositoryImpl): PersonDetailRepository

    @Binds
    @Singleton
    abstract fun bindStudioDetailRepository(impl: StudioDetailRepositoryImpl): StudioDetailRepository
}
