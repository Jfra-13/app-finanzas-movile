package com.example.finanzas_independientes_app.presentation.transacciones

import com.example.finanzas_independientes_app.core.network.ApiCode
import com.example.finanzas_independientes_app.core.network.ApiResult
import com.example.finanzas_independientes_app.core.network.AppError
import com.example.finanzas_independientes_app.domain.model.Categoria
import com.example.finanzas_independientes_app.domain.model.PaginatedTransacciones
import com.example.finanzas_independientes_app.domain.model.Transaccion
import com.example.finanzas_independientes_app.domain.repository.FinanzasRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

/**
 * A successful delete or update must reload the list and release the loading flag.
 * Before the fix, both left isLoading = true, so the reload (and every later
 * cargar/cargarMas) returned early until the ViewModel was recreated. The reload
 * must also drop a page load still in flight, or that page lands on the fresh list.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TransaccionesRecargaTest {

    // Only the calls this ViewModel makes are implemented; anything else throws.
    private class FakeRepo : FinanzasRepository by notNeeded() {
        var pagina: List<Transaccion> = emptyList()
        var ultimaPagina = true
        /** A request for a page listed here suspends until its deferred completes (one-shot). */
        val bloqueos = mutableMapOf<Int, CompletableDeferred<List<Transaccion>>>()
        /** When set, a cancelled request reports back only once this completes. */
        var respuestaCancelada: CompletableDeferred<Unit>? = null
        var listarCalls = 0
            private set

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
            val contenido = bloqueos.remove(page)?.let { bloqueo ->
                try {
                    bloqueo.await()
                } catch (e: CancellationException) {
                    // Mimics safeApiCall, which catches the cancellation and returns an error.
                    withContext(NonCancellable) { respuestaCancelada?.await() }
                    return ApiResult.Error(AppError.Unexpected(e))
                }
            } ?: pagina
            return ApiResult.Success(
                PaginatedTransacciones(contenido, contenido.size.toLong(), 2, page, 20, page == 0, ultimaPagina),
                ApiCode.TRANSACTIONS_OK
            )
        }

        override suspend fun listarCategorias(): ApiResult<List<Categoria>> =
            ApiResult.Success(emptyList(), ApiCode.CATEGORIES_OK)

        override suspend fun eliminarTransaccion(id: Long): ApiResult<Unit> =
            ApiResult.Success(Unit, ApiCode.TRANSACTION_DELETED)

        override suspend fun actualizarTransaccion(
            id: Long,
            monto: Double,
            tipo: String,
            descripcion: String?,
            fecha: String?,
            categoriaId: Long?
        ): ApiResult<Unit> = ApiResult.Success(Unit, ApiCode.TRANSACTION_UPDATED)

        companion object {
            private fun notNeeded(): FinanzasRepository = Proxy.newProxyInstance(
                FinanzasRepository::class.java.classLoader,
                arrayOf(FinanzasRepository::class.java)
            ) { _, method, _ ->
                throw UnsupportedOperationException("${method.name} not used by TransaccionesRecargaTest")
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
    fun `eliminar con exito recarga la lista y apaga el spinner`() {
        repo.pagina = listOf(tx(1), tx(2))
        val vm = TransaccionesViewModel(repo)
        repo.pagina = listOf(tx(2))

        vm.eliminar(1)

        assertEquals(2, repo.listarCalls)
        assertEquals(listOf(tx(2)), vm.transacciones.value)
        assertFalse(vm.isLoading.value)
    }

    @Test
    fun `despues de eliminar, cargarMas vuelve a pedir la pagina siguiente`() {
        repo.ultimaPagina = false
        repo.pagina = listOf(tx(1), tx(2))
        val vm = TransaccionesViewModel(repo)

        vm.eliminar(1)
        vm.cargarMas()

        assertEquals(3, repo.listarCalls)
    }

    @Test
    fun `la carga de pagina en vuelo se descarta al recargar`() {
        repo.ultimaPagina = false
        repo.pagina = listOf(tx(1), tx(2))
        val vm = TransaccionesViewModel(repo)
        val paginaTardia = CompletableDeferred<List<Transaccion>>()
        val respuestaCancelada = CompletableDeferred<Unit>()
        val recarga = CompletableDeferred<List<Transaccion>>()
        repo.bloqueos[1] = paginaTardia
        repo.respuestaCancelada = respuestaCancelada
        vm.cargarMas() // suspended on page 1

        repo.bloqueos[0] = recarga
        vm.eliminar(1) // reload starts and suspends on page 0

        // The dropped page load reports its swallowed cancellation while the reload is pending.
        respuestaCancelada.complete(Unit)
        assertTrue(vm.isLoading.value)
        assertNull(vm.mensajeError.value)

        recarga.complete(listOf(tx(2)))
        paginaTardia.complete(listOf(tx(9))) // only lands if the page load was not cancelled

        assertEquals(listOf(tx(2)), vm.transacciones.value)
        assertEquals(1, vm.currentPage.value)
        assertFalse(vm.isLoading.value)
        assertNull(vm.mensajeError.value)
    }

    @Test
    fun `actualizar con exito recarga la lista y apaga el spinner`() {
        repo.pagina = listOf(tx(1))
        val vm = TransaccionesViewModel(repo)
        repo.pagina = listOf(tx(1), tx(3))

        vm.actualizar(1, 25.0, "EGRESO", null, null)

        assertEquals(2, repo.listarCalls)
        assertEquals(listOf(tx(1), tx(3)), vm.transacciones.value)
        assertFalse(vm.isLoading.value)
    }
}
