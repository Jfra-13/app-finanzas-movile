package com.example.finanzas_independientes_app

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.finanzas_independientes_app.data.local.AppDatabase
import com.example.finanzas_independientes_app.data.local.UserDataWiper
import com.example.finanzas_independientes_app.data.local.entity.TransaccionEntity
import kotlinx.coroutines.runBlocking
import okhttp3.Cache
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Pins that ending a session leaves no cached rows for the next account. */
@RunWith(AndroidJUnit4::class)
class UserDataWiperTest {

    @Test
    fun wipeRemovesThePreviousUsersCachedHistory() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val cache = Cache(File(context.cacheDir, "wiper_test_cache"), 1024 * 1024)
        db.transaccionDao().upsertAll(
            listOf(TransaccionEntity(1, 50.0, "INGRESO", null, "2026-10-01", null, null, usuarioId = 7))
        )

        UserDataWiper(db, cache).wipe()

        assertTrue(db.transaccionDao().getAll().isEmpty())
        db.close()
        cache.delete()
    }
}
