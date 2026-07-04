package com.lambao.animike.di

import com.lambao.animike.data.repository.AnimeDetailRepository
import com.lambao.animike.data.repository.AnimeDetailRepositoryImpl
import com.lambao.animike.data.repository.AnimeRepository
import com.lambao.animike.data.repository.AnimeRepositoryImpl
import com.lambao.animike.data.repository.SearchRepository
import com.lambao.animike.data.repository.SearchRepositoryImpl
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
}
