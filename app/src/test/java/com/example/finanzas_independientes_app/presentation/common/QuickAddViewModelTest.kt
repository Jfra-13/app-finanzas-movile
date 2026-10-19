package com.example.finanzas_independientes_app.presentation.common

import com.example.finanzas_independientes_app.core.network.ApiCode
import com.example.finanzas_independientes_app.core.network.ApiResult
import com.example.finanzas_independientes_app.domain.model.Categoria
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

/** Pins quick-add validation, the payload sent to the repository and the per-type success message. */
@OptIn(ExperimentalCoroutinesApi::class)
class QuickAddViewModelTest {

    private val repo = FakeFinanzasRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `monto no numerico no llega a la red`() {
        val viewModel = QuickAddViewModel(repo)

        viewModel.registrarTransaccion("abc", "INGRESO", null, null)

        assertTrue(repo.registrarTransaccionCalls.isEmpty())
        assertEquals("Ingresá un monto válido (mayor a 0)", viewModel.mensajeUI.value)
    }

    @Test
    fun `monto cero no llega a la red`() {
        val viewModel = QuickAddViewModel(repo)

        viewModel.registrarTransaccion("0", "EGRESO", null, null)

        assertTrue(repo.registrarTransaccionCalls.isEmpty())
        assertEquals("Ingresá un monto válido (mayor a 0)", viewModel.mensajeUI.value)
    }

    @Test
    fun `monto negativo no llega a la red`() {
        val viewModel = QuickAddViewModel(repo)

        viewModel.registrarTransaccion("-5", "EGRESO", null, null)

        assertTrue(repo.registrarTransaccionCalls.isEmpty())
        assertEquals("Ingresá un monto válido (mayor a 0)", viewModel.mensajeUI.value)
    }

    @Test
    fun `ingreso exitoso envia monto recortado y descripcion blanca como null`() {
        val viewModel = QuickAddViewModel(repo)

        viewModel.registrarTransaccion(" 150.5 ", "INGRESO", "   ", 7L)

        assertEquals(
            listOf(FakeFinanzasRepository.RegistrarTransaccionCall(150.5, "INGRESO", null, null, 7L)),
            repo.registrarTransaccionCalls
        )
        assertEquals("¡Ingreso registrado!", viewModel.mensajeUI.value)
    }

    @Test
    fun `egreso exitoso conserva la descripcion y muestra su propio mensaje`() {
        val viewModel = QuickAddViewModel(repo)

        viewModel.registrarTransaccion("20", "EGRESO", "café", null)

        assertEquals("café", repo.registrarTransaccionCalls.single().descripcion)
        assertEquals("Egreso registrado.", viewModel.mensajeUI.value)
    }

    @Test
    fun `error de validacion se decide por codigo y se intento registrar`() {
        repo.registrarTransaccionResult = apiError(ApiCode.VALIDATION_ERROR)
        val viewModel = QuickAddViewModel(repo)

        viewModel.registrarTransaccion("10", "INGRESO", null, null)

        assertEquals(1, repo.registrarTransaccionCalls.size)
        assertEquals("Datos inválidos. Revisá los campos.", viewModel.mensajeUI.value)
    }

    @Test
    fun `sesion expirada muestra su mensaje y se intento registrar`() {
        repo.registrarTransaccionResult = apiError(ApiCode.UNAUTHORIZED)
        val viewModel = QuickAddViewModel(repo)

        viewModel.registrarTransaccion("10", "INGRESO", null, null)

        assertEquals(1, repo.registrarTransaccionCalls.size)
        assertEquals("Sesión expirada. Volvé a iniciar sesión.", viewModel.mensajeUI.value)
    }

    @Test
    fun `codigo no mapeado cae en el mensaje con el codigo crudo`() {
        repo.registrarTransaccionResult = apiError(ApiCode.INTERNAL_SERVER_ERROR)
        val viewModel = QuickAddViewModel(repo)

        viewModel.registrarTransaccion("10", "INGRESO", null, null)

        assertEquals(1, repo.registrarTransaccionCalls.size)
        assertEquals(
            "Error del servidor: ${ApiCode.INTERNAL_SERVER_ERROR.raw}",
            viewModel.mensajeUI.value
        )
    }

    @Test
    fun `cargar categorias expone la lista y un error posterior la deja intacta sin mensaje`() {
        val categorias = listOf(Categoria(1, "Comida", "EGRESO"))
        repo.listarCategoriasResult = ApiResult.Success(categorias, ApiCode.CATEGORIES_OK)
        val viewModel = QuickAddViewModel(repo)

        viewModel.cargarCategorias()
        repo.listarCategoriasResult = apiError(ApiCode.INTERNAL_SERVER_ERROR)
        viewModel.cargarCategorias()

        assertEquals(2, repo.listarCategoriasCalls)
        assertEquals(categorias, viewModel.categorias.value)
        assertNull(viewModel.mensajeUI.value)
    }
}
