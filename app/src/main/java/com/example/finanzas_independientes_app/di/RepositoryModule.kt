package com.example.finanzas_independientes_app.di

import com.example.finanzas_independientes_app.data.repository.AuthRepositoryImpl
import com.example.finanzas_independientes_app.data.repository.FinanzasRepositoryImpl
import com.example.finanzas_independientes_app.domain.repository.AuthRepository
import com.example.finanzas_independientes_app.domain.repository.FinanzasRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds repository interfaces to their data-layer implementations. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindFinanzasRepository(impl: FinanzasRepositoryImpl): FinanzasRepository
}
