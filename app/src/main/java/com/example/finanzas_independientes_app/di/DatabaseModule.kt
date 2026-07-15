package com.example.finanzas_independientes_app.di

import android.content.Context
import androidx.room.Room
import com.example.finanzas_independientes_app.data.local.AppDatabase
import com.example.finanzas_independientes_app.data.local.dao.CategoriaDao
import com.example.finanzas_independientes_app.data.local.dao.MetaDao
import com.example.finanzas_independientes_app.data.local.dao.TransaccionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provides the Room database and its DAOs as process singletons. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideTransaccionDao(db: AppDatabase): TransaccionDao = db.transaccionDao()

    @Provides
    fun provideCategoriaDao(db: AppDatabase): CategoriaDao = db.categoriaDao()

    @Provides
    fun provideMetaDao(db: AppDatabase): MetaDao = db.metaDao()
}
