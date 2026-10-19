package com.example.finanzas_independientes_app.di

import android.content.Context
import android.util.Base64
import androidx.room.Room
import com.example.finanzas_independientes_app.core.session.EncryptedSecureStorage
import com.example.finanzas_independientes_app.core.session.SecureStorage
import com.example.finanzas_independientes_app.data.local.AppDatabase
import com.example.finanzas_independientes_app.data.local.dao.CategoriaDao
import com.example.finanzas_independientes_app.data.local.dao.MetaDao
import com.example.finanzas_independientes_app.data.local.dao.TransaccionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.security.SecureRandom
import javax.inject.Singleton

/** Provides the SQLCipher-encrypted Room database and its DAOs as process singletons. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    // Separate from the session prefs: logout clears those, and losing this key
    // would leave finanzas.db unreadable.
    private const val KEY_FILE = "finanzas_db_key"
    private const val KEY_PASSPHRASE = "db_passphrase"
    private const val PASSPHRASE_BYTES = 32 // 256-bit

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        System.loadLibrary("sqlcipher")
        val passphrase = databasePassphrase(context, EncryptedSecureStorage(context, KEY_FILE))
        return Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .openHelperFactory(SupportOpenHelperFactory(passphrase))
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    /**
     * Returns the database key, generating a random one on first use. No stored
     * key means any existing file is unreadable (plain SQLite from builds before
     * encryption, or a lost key), so it is deleted and Room recreates it. Safe
     * because the database is only a cache of server data.
     */
    internal fun databasePassphrase(context: Context, storage: SecureStorage): ByteArray {
        storage.getString(KEY_PASSPHRASE)?.let { return Base64.decode(it, Base64.NO_WRAP) }
        context.deleteDatabase(AppDatabase.NAME)
        val passphrase = ByteArray(PASSPHRASE_BYTES).also { SecureRandom().nextBytes(it) }
        storage.putString(KEY_PASSPHRASE, Base64.encodeToString(passphrase, Base64.NO_WRAP))
        return passphrase
    }

    @Provides
    fun provideTransaccionDao(db: AppDatabase): TransaccionDao = db.transaccionDao()

    @Provides
    fun provideCategoriaDao(db: AppDatabase): CategoriaDao = db.categoriaDao()

    @Provides
    fun provideMetaDao(db: AppDatabase): MetaDao = db.metaDao()
}
