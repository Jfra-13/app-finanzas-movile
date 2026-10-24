package com.example.finanzas_independientes_app.presentation.transacciones

import com.example.finanzas_independientes_app.core.network.ApiCode
import com.example.finanzas_independientes_app.core.network.ApiResult
import com.example.finanzas_independientes_app.domain.model.Categoria
import com.example.finanzas_independientes_app.domain.model.PaginatedTransacciones
import com.example.finanzas_independientes_app.domain.model.Transaccion
import com.example.finanzas_independientes_app.fakes.FakeFinanzasRepository
import com.example.finanzas_independientes_app.fakes.apiError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pins paging, the date range filter (including its client-side guard), the delete/update
 * request shapes and the per-operation error mapping with its own fallbacks.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TransaccionesViewModelTest {

    private val repo = FakeFinanzasRepository()

    private fun tx(id: Long, monto: Double = 10.0) =
        Transaccion(id, monto, "INGRESO", null, "2026-03-01", null, null, 1)

    private fun pagina(content: List<Transaccion>, number: Int, last: Boolean) =
        ApiResult.Success(
            PaginatedTransacciones(content, content.size.toLong(), 2, number, 20, number == 0, last),
            ApiCode.TRANSACTIONS_OK
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = TransaccionesViewModel(repo)

    @Test
    fun `al crearse pide la primera pagina sin filtro, avanza el cursor y carga categorias`() {
        val cats = listOf(Categoria(1, "Comida", "EGRESO"))
        repo.listarCategoriasResult = ApiResult.Success(cats, ApiCode.CATEGORIES_OK)
        repo.listarTransaccionesResult = pagina(listOf(tx(1), tx(2)), number = 0, last = false)

        val vm = viewModel()

        assertEquals(
            listOf(
                FakeFinanzasRepository.ListarTransaccionesCall(
                    null, null, null, null, null, page = 0, size = 20, sort = "fecha,desc"
                )
            ),
            repo.listarTransaccionesCalls
        )
        assertEquals(listOf(tx(1), tx(2)), vm.transacciones.value)
        assertEquals(1, vm.currentPage.value)
        assertEquals(cats, vm.categorias.value)
    }

    @Test
    fun `cargarMas agrega la siguiente pagina y se detiene en la ultima, y refrescar vuelve a la pagina 0`() {
        repo.listarTransaccionesResult = pagina(listOf(tx(1)), number = 0, last = false)
        val vm = viewModel()

        repo.listarTransaccionesResult = pagina(listOf(tx(2)), number = 1, last = true)
        vm.cargarMas()

        assertEquals(1, repo.listarTransaccionesCalls.last().page)
        assertEquals(listOf(tx(1), tx(2)), vm.transacciones.value)
        assertEquals(2, vm.currentPage.value)
        assertTrue(vm.isLastPage.value)

        vm.cargarMas()
        vm.cargar(refresh = false)
        assertEquals(2, repo.listarTransaccionesCalls.size)

        repo.listarTransaccionesResult = pagina(listOf(tx(3)), number = 0, last = false)
        vm.cargar(refresh = true)

        assertEquals(0, repo.listarTransaccionesCalls.last().page)
        assertEquals(listOf(tx(3)), vm.transacciones.value)
        assertEquals(1, vm.currentPage.value)
        assertEquals(false, vm.isLastPage.value)
    }

    @Test
    fun `una respuesta vacia marca isEmpty`() {
        repo.listarTransaccionesResult = pagina(emptyList(), number = 0, last = true)

        val vm = viewModel()

        assertTrue(vm.isEmpty.value)
    }

    @Test
    fun `los errores de carga marcan isEmpty, conservan la lista previa y usan su propio mapeo`() {
        repo.listarTransaccionesResult = apiError(ApiCode.UNAUTHORIZED)
        val vm = viewModel()

        assertTrue(vm.isEmpty.value)
        assertEquals("Sesión expirada. Volvé a iniciar sesión.", vm.mensajeError.value)

        repo.listarTransaccionesResult = pagina(listOf(tx(1)), number = 0, last = false)
        vm.cargar(refresh = true)
        assertEquals(false, vm.isEmpty.value)

        repo.listarTransaccionesResult = apiError(ApiCode.VALIDATION_ERROR)
        vm.cargar(refresh = true)

        assertEquals(3, repo.listarTransaccionesCalls.size)
        assertEquals(listOf(tx(1)), vm.transacciones.value)
        assertEquals("Error al cargar transacciones.", vm.mensajeError.value)

        vm.limpiarError()
        assertNull(vm.mensajeError.value)
    }

    @Test
    fun `aplicar un filtro manda las fechas, acepta desde igual a hasta y rechaza localmente un rango invertido`() {
        repo.listarTransaccionesResult = pagina(listOf(tx(1)), number = 0, last = false)
        val vm = viewModel()
        assertEquals(1, vm.currentPage.value)

        vm.aplicarFiltroFechas("2026-03-01", "2026-03-31")

        assertEquals("2026-03-01" to "2026-03-31", vm.filtroFechas.value)
        with(repo.listarTransaccionesCalls.last()) {
            assertEquals("2026-03-01", desde)
            assertEquals("2026-03-31", hasta)
            assertEquals(0, page)
        }

        vm.aplicarFiltroFechas("2026-03-15", "2026-03-15")
        assertEquals("2026-03-15" to "2026-03-15", vm.filtroFechas.value)
        assertEquals(3, repo.listarTransaccionesCalls.size)

        vm.aplicarFiltroFechas("2026-04-10", "2026-04-01")

        assertEquals("El rango de fechas no es válido.", vm.mensajeError.value)
        assertEquals("2026-03-15" to "2026-03-15", vm.filtroFechas.value)
        assertEquals(3, repo.listarTransaccionesCalls.size)
    }

    @Test
    fun `limpiar el filtro recarga sin fechas, y sin filtro activo no hace nada`() {
        val vm = viewModel()
        vm.limpiarFiltroFechas()
        assertEquals(1, repo.listarTransaccionesCalls.size)

        vm.aplicarFiltroFechas("2026-03-01", "2026-03-31")
        vm.limpiarFiltroFechas()

        assertNull(vm.filtroFechas.value)
        assertEquals(3, repo.listarTransaccionesCalls.size)
        with(repo.listarTransaccionesCalls.last()) {
            assertNull(desde)
            assertNull(hasta)
        }
    }

    @Test
    fun `eliminar manda el id y quita la transaccion de la lista local`() {
        repo.listarTransaccionesResult = pagina(listOf(tx(1)), number = 0, last = true)
        val vm = viewModel()
        repo.listarTransaccionesResult = pagina(emptyList(), number = 0, last = true)

        vm.eliminar(1)

        assertEquals(listOf(1L), repo.eliminarTransaccionCalls)
        assertTrue(vm.transacciones.value.isEmpty())
        assertTrue(vm.isEmpty.value)
    }

    @Test
    fun `eliminar con error conserva la lista, no recarga y mapea cada codigo`() {
        repo.listarTransaccionesResult = pagina(listOf(tx(1)), number = 0, last = true)
        val vm = viewModel()

        val esperados = listOf(
            ApiCode.ACCESO_DENEGADO to "No tenés permiso para eliminar esta transacción.",
            ApiCode.VALIDATION_ERROR to "Error al eliminar la transacción."
        )
        for ((code, mensaje) in esperados) {
            repo.eliminarTransaccionResult = apiError(code)
            vm.eliminar(1)
            assertEquals(mensaje, vm.mensajeError.value)
        }

        assertEquals(listOf(1L, 1L), repo.eliminarTransaccionCalls)
        assertEquals(listOf(tx(1)), vm.transacciones.value)
        assertEquals(1, repo.listarTransaccionesCalls.size)
    }

    @Test
    fun `actualizar manda los datos y convierte la descripcion en blanco en null`() {
        val vm = viewModel()

        vm.actualizar(7, 25.5, "EGRESO", "   ", categoriaId = null)
        vm.actualizar(8, 40.0, "INGRESO", "Venta", categoriaId = 3)

        assertEquals(
            listOf(
                FakeFinanzasRepository.ActualizarTransaccionCall(7, 25.5, "EGRESO", null, null, null),
                FakeFinanzasRepository.ActualizarTransaccionCall(8, 40.0, "INGRESO", "Venta", null, 3)
            ),
            repo.actualizarTransaccionCalls
        )
    }

    @Test
    fun `actualizar con error no recarga y mapea cada codigo`() {
        val vm = viewModel()

        val esperados = listOf(
            ApiCode.VALIDATION_ERROR to "Datos inválidos. Revisá los campos.",
            ApiCode.PARAMETRO_INVALIDO to "Error al actualizar la transacción."
        )
        for ((code, mensaje) in esperados) {
            repo.actualizarTransaccionResult = apiError(code)
            vm.actualizar(7, 25.5, "EGRESO", null, null)
            assertEquals(mensaje, vm.mensajeError.value)
        }

        assertEquals(2, repo.actualizarTransaccionCalls.size)
        assertEquals(1, repo.listarTransaccionesCalls.size)
    }
}
