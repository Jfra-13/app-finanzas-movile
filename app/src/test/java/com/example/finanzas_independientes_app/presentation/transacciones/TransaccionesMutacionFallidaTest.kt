package com.example.finanzas_independientes_app.presentation.transacciones

import com.example.finanzas_independientes_app.core.network.ApiCode
import com.example.finanzas_independientes_app.core.network.ApiResult
import com.example.finanzas_independientes_app.core.network.AppError
import com.example.finanzas_independientes_app.domain.model.Categoria
import com.example.finanzas_independientes_app.domain.model.PaginatedTransacciones
import com.example.finanzas_independientes_app.domain.model.Transaccion
import com.example.finanzas_independientes_app.domain.repository.FinanzasRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

/**
 * A failed delete or update must not release the loading flag while a list load
 * is still running. Before the fix it did, so cargarMas started a second load
 * next to the first and appended a page from the old state onto the list.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TransaccionesMutacionFallidaTest {

    // Only the calls this ViewModel makes are implemented; anything else throws.
    private class FakeRepo : FinanzasRepository by notNeeded() {
        /** The next request suspends until this completes (one-shot). */
        var bloqueo: CompletableDeferred<List<Transaccion>>? = null
        var listarCalls = 0
            private set
        private val falla = ApiResult.Error(AppError.Network)

        override suspend fun listarTransacciones(
            tipo: String?,
            categoriaId: Long?,
            sinCategoria: Boolean?,
            desde: String?,
            hasta: String?,
            page: Int,
            size: Int,
            sort: String?
        ): ApiResult<PaginatedTransacciones> {
            listarCalls++
            val contenido = bloqueo?.also { bloqueo = null }?.await() ?: emptyList()
            return ApiResult.Success(
                PaginatedTransacciones(contenido, contenido.size.toLong(), 2, page, 20, page == 0, false),
                ApiCode.TRANSACTIONS_OK
            )
        }

        override suspend fun listarCategorias(): ApiResult<List<Categoria>> =
            ApiResult.Success(emptyList(), ApiCode.CATEGORIES_OK)

        override suspend fun eliminarTransaccion(id: Long): ApiResult<Unit> = falla

        override suspend fun actualizarTransaccion(
            id: Long,
            monto: Double,
            tipo: String,
            descripcion: String?,
            fecha: String?,
            categoriaId: Long?
        ): ApiResult<Unit> = falla

        companion object {
            private fun notNeeded(): FinanzasRepository = Proxy.newProxyInstance(
                FinanzasRepository::class.java.classLoader,
                arrayOf(FinanzasRepository::class.java)
            ) { _, method, _ ->
                throw UnsupportedOperationException("${method.name} not used by TransaccionesMutacionFallidaTest")
            } as FinanzasRepository
        }
    }

    private val repo = FakeRepo()

    private fun tx(id: Long) = Transaccion(id, 10.0, "INGRESO", null, "2026-03-01", null, null, 1)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `eliminar fallido durante una carga no abre la puerta a otra carga`() {
        val carga = CompletableDeferred<List<Transaccion>>()
        repo.bloqueo = carga
        val vm = TransaccionesViewModel(repo) // initial load suspended

        vm.eliminar(1)
        vm.cargarMas()

        assertTrue(vm.isLoading.value)
        assertEquals(1, repo.listarCalls)

        carga.complete(listOf(tx(1)))
        assertFalse(vm.isLoading.value)
        assertEquals(listOf(tx(1)), vm.transacciones.value)
    }

    @Test
    fun `actualizar fallido durante una carga no abre la puerta a otra carga`() {
        val carga = CompletableDeferred<List<Transaccion>>()
        repo.bloqueo = carga
        val vm = TransaccionesViewModel(repo)

        vm.actualizar(1, 25.0, "EGRESO", null, null)
        vm.cargarMas()

        assertTrue(vm.isLoading.value)
        assertEquals(1, repo.listarCalls)

        carga.complete(listOf(tx(1)))
        assertFalse(vm.isLoading.value)
    }

    @Test
    fun `eliminar fallido sin carga en curso apaga el spinner`() {
        val vm = TransaccionesViewModel(repo)

        vm.eliminar(1)

        assertFalse(vm.isLoading.value)
    }
}
