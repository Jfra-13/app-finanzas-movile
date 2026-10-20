package com.example.finanzas_independientes_app.presentation.transacciones

import com.example.finanzas_independientes_app.core.network.ApiCode
import com.example.finanzas_independientes_app.core.network.ApiResult
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
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

/**
 * Changing the date filter while a load is in flight must reload with the new
 * range. Before the fix, aplicarFiltroFechas/limpiarFiltroFechas hit cargar()'s
 * isLoading guard and returned, so the list kept the old range.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TransaccionesFiltroTest {

    // Only the calls this ViewModel makes are implemented; anything else throws.
    private class FakeRepo : FinanzasRepository by notNeeded() {
        /** Each request's (desde, hasta), in call order. */
        val rangos = mutableListOf<Pair<String?, String?>>()
        /** The next request suspends until this completes (one-shot). */
        var bloqueo: CompletableDeferred<List<Transaccion>>? = null
        var pagina: List<Transaccion> = emptyList()

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
            rangos += desde to hasta
            val contenido = bloqueo?.also { bloqueo = null }?.await() ?: pagina
            return ApiResult.Success(
                PaginatedTransacciones(contenido, contenido.size.toLong(), 1, page, 20, true, true),
                ApiCode.TRANSACTIONS_OK
            )
        }

        override suspend fun listarCategorias(): ApiResult<List<Categoria>> =
            ApiResult.Success(emptyList(), ApiCode.CATEGORIES_OK)

        companion object {
            private fun notNeeded(): FinanzasRepository = Proxy.newProxyInstance(
                FinanzasRepository::class.java.classLoader,
                arrayOf(FinanzasRepository::class.java)
            ) { _, method, _ ->
                throw UnsupportedOperationException("${method.name} not used by TransaccionesFiltroTest")
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
    fun `aplicar un filtro durante una carga recarga con el rango nuevo`() {
        val cargaInicial = CompletableDeferred<List<Transaccion>>()
        repo.bloqueo = cargaInicial
        val vm = TransaccionesViewModel(repo) // initial load suspended
        repo.pagina = listOf(tx(2))

        vm.aplicarFiltroFechas("2026-03-01", "2026-03-31")
        cargaInicial.complete(listOf(tx(1), tx(9))) // only lands if the old load survived

        assertEquals(listOf<Pair<String?, String?>>(null to null, "2026-03-01" to "2026-03-31"), repo.rangos)
        assertEquals(listOf(tx(2)), vm.transacciones.value)
        assertFalse(vm.isLoading.value)
    }

    @Test
    fun `limpiar el filtro durante una carga recarga sin rango`() {
        val vm = TransaccionesViewModel(repo)
        vm.aplicarFiltroFechas("2026-03-01", "2026-03-31")
        val cargaFiltrada = CompletableDeferred<List<Transaccion>>()
        repo.bloqueo = cargaFiltrada
        vm.aplicarFiltroFechas("2026-04-01", "2026-04-30") // suspended
        repo.pagina = listOf(tx(3))

        vm.limpiarFiltroFechas()
        cargaFiltrada.complete(listOf(tx(9)))

        assertEquals(null to null, repo.rangos.last())
        assertEquals(listOf(tx(3)), vm.transacciones.value)
        assertFalse(vm.isLoading.value)
    }
}
