package com.example.finanzas_independientes_app.presentation.categorias

import com.example.finanzas_independientes_app.core.network.ApiCode
import com.example.finanzas_independientes_app.core.network.ApiResult
import com.example.finanzas_independientes_app.domain.model.Categoria
import com.example.finanzas_independientes_app.domain.model.Presupuesto
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
 * Pins category/budget joining on load, name and amount validation, the CRUD flows that
 * reload on success, and the per-operation error mapping (each with its own fallback).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CategoriasViewModelTest {

    private val repo = FakeFinanzasRepository()

    private val comida = Categoria(1, "Comida", "EGRESO")
    private val ventas = Categoria(2, "Ventas", "INGRESO")

    private fun presupuesto(categoriaId: Long, monto: Double = 300.0) =
        Presupuesto(10, categoriaId, "Comida", monto, 50.0, monto - 50.0, 16.0, false)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = CategoriasViewModel(repo)

    @Test
    fun `al crearse une cada categoria con su presupuesto por categoriaId`() {
        repo.listarCategoriasResult = ApiResult.Success(listOf(comida, ventas), ApiCode.CATEGORIES_OK)
        val tope = presupuesto(categoriaId = 1)
        repo.obtenerPresupuestosResult = ApiResult.Success(listOf(tope), ApiCode.BUDGETS_OK)

        val vm = viewModel()

        assertEquals(1, repo.listarCategoriasCalls)
        assertEquals(1, repo.obtenerPresupuestosCalls)
        assertEquals(
            listOf(CategoriaConPresupuesto(comida, tope), CategoriaConPresupuesto(ventas, null)),
            vm.categorias.value
        )
    }

    @Test
    fun `si fallan los presupuestos igual muestra las categorias sin tope y sin error`() {
        repo.listarCategoriasResult = ApiResult.Success(listOf(comida), ApiCode.CATEGORIES_OK)
        repo.obtenerPresupuestosResult = apiError(ApiCode.UNAUTHORIZED)

        val vm = viewModel()

        assertEquals(1, repo.obtenerPresupuestosCalls)
        assertEquals(listOf(CategoriaConPresupuesto(comida, null)), vm.categorias.value)
    }

    @Test
    fun `error al recargar conserva la lista previa y distingue sesion expirada del fallback`() {
        repo.listarCategoriasResult = ApiResult.Success(listOf(comida), ApiCode.CATEGORIES_OK)
        val vm = viewModel()
        val previa = listOf(CategoriaConPresupuesto(comida, null))
        assertEquals(previa, vm.categorias.value)

        repo.listarCategoriasResult = apiError(ApiCode.UNAUTHORIZED)
        vm.cargar()
        assertEquals("Sesión expirada. Volvé a iniciar sesión.", vm.mensajeError.value)
        assertEquals(previa, vm.categorias.value)

        repo.listarCategoriasResult = apiError(ApiCode.VALIDATION_ERROR)
        vm.cargar()

        assertEquals(3, repo.listarCategoriasCalls)
        assertEquals("Error al cargar categorías.", vm.mensajeError.value)
        assertEquals(previa, vm.categorias.value)
    }

    @Test
    fun `crear con nombre en blanco no llama al repositorio`() {
        val vm = viewModel()

        vm.crear("   ", "EGRESO")

        assertEquals("El nombre de la categoría no puede estar vacío.", vm.mensajeError.value)
        assertTrue(repo.crearCategoriaCalls.isEmpty())
    }

    @Test
    fun `crear exitoso envia el nombre recortado, avisa y recarga la lista`() {
        val vm = viewModel()
        vm.crear("   ", "EGRESO") // leaves an error to prove it is cleared
        repo.listarCategoriasResult = ApiResult.Success(listOf(comida), ApiCode.CATEGORIES_OK)

        vm.crear("  Comida  ", "EGRESO")

        assertEquals(listOf(FakeFinanzasRepository.CrearCategoriaCall("Comida", "EGRESO")), repo.crearCategoriaCalls)
        assertEquals("Categoría \"Comida\" creada.", vm.createSuccess.value)
        assertNull(vm.mensajeError.value)
        assertEquals(2, repo.listarCategoriasCalls)
        assertEquals(listOf(CategoriaConPresupuesto(comida, null)), vm.categorias.value)
    }

    @Test
    fun `crear con error mapea validacion y usa su propio fallback sin recargar`() {
        val vm = viewModel()

        repo.crearCategoriaResult = apiError(ApiCode.VALIDATION_ERROR)
        vm.crear("Comida", "EGRESO")
        assertEquals("Datos inválidos. Revisá los campos.", vm.mensajeError.value)

        repo.crearCategoriaResult = apiError(ApiCode.ACCESO_DENEGADO)
        vm.crear("Comida", "EGRESO")
        assertEquals("Error al crear la categoría.", vm.mensajeError.value)

        assertEquals(2, repo.crearCategoriaCalls.size)
        assertEquals(1, repo.listarCategoriasCalls)
    }

    @Test
    fun `renombrar con nombre en blanco no llama al repositorio`() {
        val vm = viewModel()

        vm.renombrar(1, "  ")

        assertEquals("El nombre de la categoría no puede estar vacío.", vm.mensajeError.value)
        assertTrue(repo.actualizarCategoriaCalls.isEmpty())
    }

    @Test
    fun `renombrar exitoso envia nombre recortado, avisa y recarga`() {
        val vm = viewModel()

        vm.renombrar(1, " Super ")

        assertEquals(
            listOf(FakeFinanzasRepository.ActualizarCategoriaCall(1, "Super")),
            repo.actualizarCategoriaCalls
        )
        assertEquals("Categoría renombrada.", vm.createSuccess.value)
        assertEquals(2, repo.listarCategoriasCalls)
    }

    @Test
    fun `renombrar y eliminar mapean los codigos de mutacion sin recargar`() {
        val vm = viewModel()

        repo.actualizarCategoriaResult = apiError(ApiCode.ACCESO_DENEGADO)
        vm.renombrar(1, "Super")
        assertEquals("Las categorías del sistema no se pueden modificar.", vm.mensajeError.value)

        repo.eliminarCategoriaResult = apiError(ApiCode.CATEGORIA_NO_ENCONTRADA)
        vm.eliminar(1)
        assertEquals("La categoría ya no existe. Actualizá la lista.", vm.mensajeError.value)

        repo.eliminarCategoriaResult = apiError(ApiCode.UNAUTHORIZED)
        vm.eliminar(1)
        assertEquals("Sesión expirada. Volvé a iniciar sesión.", vm.mensajeError.value)

        repo.eliminarCategoriaResult = apiError(ApiCode.VALIDATION_ERROR)
        vm.eliminar(1)
        assertEquals("Datos inválidos. Revisá los campos.", vm.mensajeError.value)

        repo.eliminarCategoriaResult = apiError(ApiCode.META_NO_ENCONTRADA)
        vm.eliminar(1)
        assertEquals("No se pudo completar la operación.", vm.mensajeError.value)

        assertEquals(1, repo.actualizarCategoriaCalls.size)
        assertEquals(listOf(1L, 1L, 1L, 1L), repo.eliminarCategoriaCalls)
        assertEquals(1, repo.listarCategoriasCalls)
    }

    @Test
    fun `eliminar exitoso avisa que los movimientos quedan sin categoria y recarga`() {
        val vm = viewModel()

        vm.eliminar(7)

        assertEquals(listOf(7L), repo.eliminarCategoriaCalls)
        assertEquals("Categoría eliminada. Sus movimientos quedan sin categoría.", vm.createSuccess.value)
        assertEquals(2, repo.listarCategoriasCalls)
    }

    @Test
    fun `fijarTope rechaza montos no numericos o menores al minimo sin llamar al repositorio`() {
        val vm = viewModel()
        val esperado = "Ingresá un monto válido (mayor a 0)."

        for (invalido in listOf("", "abc", "0", "-5", "0.009", "0,009")) {
            vm.limpiarError()
            vm.fijarTope(1, invalido)
            assertEquals("monto '$invalido'", esperado, vm.mensajeError.value)
        }

        assertTrue(repo.guardarPresupuestoCalls.isEmpty())
    }

    @Test
    fun `fijarTope acepta coma decimal y el minimo exacto de 0,01`() {
        val vm = viewModel()
        vm.fijarTope(1, "abc") // leaves an error to prove it is cleared

        vm.fijarTope(1, " 12,50 ")
        vm.fijarTope(2, "0.01")

        assertEquals(
            listOf(
                FakeFinanzasRepository.GuardarPresupuestoCall(1, 12.5),
                FakeFinanzasRepository.GuardarPresupuestoCall(2, 0.01)
            ),
            repo.guardarPresupuestoCalls
        )
        assertEquals("Tope guardado.", vm.createSuccess.value)
        assertNull(vm.mensajeError.value)
        assertEquals(3, repo.listarCategoriasCalls)
    }

    @Test
    fun `fijarTope y quitarTope con error mapean presupuesto inexistente y fallback sin recargar`() {
        val vm = viewModel()

        repo.guardarPresupuestoResult = apiError(ApiCode.CATEGORIA_NO_ENCONTRADA)
        vm.fijarTope(1, "100")
        assertEquals("La categoría ya no existe. Actualizá la lista.", vm.mensajeError.value)

        repo.eliminarPresupuestoResult = apiError(ApiCode.PRESUPUESTO_NO_ENCONTRADO)
        vm.quitarTope(10)
        assertEquals("El presupuesto ya no existe. Actualizá la lista.", vm.mensajeError.value)

        repo.eliminarPresupuestoResult = apiError(ApiCode.META_NO_ENCONTRADA)
        vm.quitarTope(10)
        assertEquals("No se pudo completar la operación.", vm.mensajeError.value)

        assertEquals(1, repo.guardarPresupuestoCalls.size)
        assertEquals(listOf(10L, 10L), repo.eliminarPresupuestoCalls)
        assertEquals(1, repo.listarCategoriasCalls)
    }

    @Test
    fun `quitarTope exitoso avisa y recarga la lista`() {
        val vm = viewModel()

        vm.quitarTope(10)

        assertEquals(listOf(10L), repo.eliminarPresupuestoCalls)
        assertEquals("Tope eliminado.", vm.createSuccess.value)
        assertEquals(2, repo.listarCategoriasCalls)
    }

    @Test
    fun `limpiarError y limpiarCreateSuccess apagan sus mensajes`() {
        val vm = viewModel()
        vm.eliminar(1) // sets the success message
        vm.crear(" ", "EGRESO") // sets the error without touching the success message
        assertEquals("El nombre de la categoría no puede estar vacío.", vm.mensajeError.value)
        assertEquals("Categoría eliminada. Sus movimientos quedan sin categoría.", vm.createSuccess.value)

        vm.limpiarError()
        vm.limpiarCreateSuccess()

        assertNull(vm.mensajeError.value)
        assertNull(vm.createSuccess.value)
    }
}
