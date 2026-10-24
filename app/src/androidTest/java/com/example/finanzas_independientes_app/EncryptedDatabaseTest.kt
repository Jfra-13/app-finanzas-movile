package com.example.finanzas_independientes_app

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.finanzas_independientes_app.core.session.SecureStorage
import com.example.finanzas_independientes_app.data.local.AppDatabase
import com.example.finanzas_independientes_app.data.local.entity.TransaccionEntity
import com.example.finanzas_independientes_app.di.DatabaseModule
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

/** Pins that finanzas.db is encrypted at rest and that its key handling is safe. */
@RunWith(AndroidJUnit4::class)
class EncryptedDatabaseTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @After
    fun tearDown() {
        context.deleteDatabase(AppDatabase.NAME)
    }

    @Test
    fun databaseFileCannotBeReadAsPlainSqlite() = runBlocking {
        val db = DatabaseModule.provideDatabase(context)
        db.transaccionDao().upsertAll(
            listOf(TransaccionEntity(1, 50.0, "INGRESO", "secret", "2026-10-01", null, null, usuarioId = 7))
        )
        db.close()

        // Every plain SQLite file starts with this header; SQLCipher's starts with random salt.
        val header = context.getDatabasePath(AppDatabase.NAME).inputStream().use { input -> ByteArray(16).also { input.read(it) } }
        assertFalse(header.contentEquals("SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)))
    }

    @Test
    fun passphraseIsGeneratedOnceAndReused() {
        val storage = InMemoryStorage()

        val first = DatabaseModule.databasePassphrase(context, storage)
        val second = DatabaseModule.databasePassphrase(context, storage)

        assertEquals(32, first.size)
        assertArrayEquals(first, second)
    }

    @Test
    fun missingKeyDeletesTheUnreadableOldFile() {
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(AppDatabase.NAME), null).close()

        DatabaseModule.databasePassphrase(context, InMemoryStorage())

        assertFalse(context.getDatabasePath(AppDatabase.NAME).exists())
    }

    private class InMemoryStorage : SecureStorage {
        private val values = mutableMapOf<String, Any>()
        override fun putString(key: String, value: String) { values[key] = value }
        override fun getString(key: String): String? = values[key] as String?
        override fun putLong(key: String, value: Long) { values[key] = value }
        override fun getLong(key: String): Long? = values[key] as Long?
        override fun remove(key: String) { values.remove(key) }
        override fun clear() = values.clear()
    }
}
