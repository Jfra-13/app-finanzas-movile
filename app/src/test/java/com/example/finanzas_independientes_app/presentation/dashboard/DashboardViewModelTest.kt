package com.example.finanzas_independientes_app.presentation.dashboard

import com.example.finanzas_independientes_app.core.network.ApiCode
import com.example.finanzas_independientes_app.core.network.ApiResult
import com.example.finanzas_independientes_app.domain.model.Categoria
import com.example.finanzas_independientes_app.domain.model.ProgresoMetas
import com.example.finanzas_independientes_app.domain.model.ResumenSemanalDia
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
import java.util.Locale

/**
 * Pins how the dashboard derives its display strings from the parallel loads (including the
 * META_NO_ENCONTRADA special cases), the validation of amounts and goal days, and the
 * mutations that refresh the whole screen on success.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val repo = FakeFinanzasRepository()
    private lateinit var vm: DashboardViewModel
    private lateinit var previousLocale: Locale

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        // Production formats with String.format and the default locale; pin it for stable output.
        previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
        vm = DashboardViewModel(repo)
    }

    @After
    fun tearDown() {
        Locale.setDefault(previousLocale)
        Dispatchers.resetMain()
    }

    @Test
    fun `cargarResumen llena todos los flujos con el formato esperado`() {
        val progreso = ProgresoMetas(10.0, 50.0, 70.0, 350.0, 300.0, 1500.0)
        val semana = listOf(ResumenSemanalDia("LUN", 40.0, 15.0))
        repo.obtenerHoyResult = ApiResult.Success(12.345, ApiCode.TODAY_INCOME_OK)
        repo.obtenerCuotaDiariaResult = ApiResult.Success(150.6, ApiCode.DAILY_QUOTA_OK)
        repo.obtenerProgresoMetasResult = ApiResult.Success(progreso, ApiCode.GOALS_PROGRESS_OK)
        repo.obtenerResumenSemanalResult = ApiResult.Success(semana, ApiCode.WEEKLY_SUMMARY_OK)

        vm.cargarResumen()

        assertEquals("12.35", vm.ingresoHoy.value)
        assertEquals("151 S/", vm.cuotaActual.value)
        assertEquals(progreso, vm.progresoMetas.value)
        assertEquals(semana, vm.resumenSemanal.value)
        assertEquals(1, repo.obtenerHoyCalls)
        assertEquals(1, repo.obtenerCuotaDiariaCalls)
        assertEquals(1, repo.obtenerProgresoMetasCalls)
        assertEquals(1, repo.obtenerResumenSemanalCalls)
    }

    @Test
    fun `cuota negativa o cero se muestra como meta superada con valor absoluto`() {
        repo.obtenerCuotaDiariaResult = ApiResult.Success(-25.5, ApiCode.DAILY_QUOTA_OK)
        vm.cargarResumen()
        assertEquals("¡Meta superada por S/ 25.50! 🎉", vm.cuotaActual.value)

        repo.obtenerCuotaDiariaResult = ApiResult.Success(0.0, ApiCode.DAILY_QUOTA_OK)
        vm.cargarResumen()
        assertEquals("¡Meta superada por S/ 0.00! 🎉", vm.cuotaActual.value)
    }

    @Test
    fun `sin meta en la cuota muestra sin meta activa y el aviso para fijarla`() {
        repo.obtenerCuotaDiariaResult = apiError(ApiCode.META_NO_ENCONTRADA)
        repo.obtenerProgresoMetasResult = apiError(ApiCode.META_NO_ENCONTRADA)

        vm.cargarResumen()

        assertEquals("Sin meta activa", vm.cuotaActual.value)
        // Progress with the same code must not overwrite the message with the generic one.
        assertEquals(
            "Aún no tienes una meta mensual. ¡Fijá una para activar el motor de metas!",
            vm.mensajeUI.value
        )
        assertEquals(1, repo.obtenerProgresoMetasCalls)
    }

    @Test
    fun `progreso sin meta limpia un progreso previo`() {
        val progreso = ProgresoMetas(10.0, 50.0, 70.0, 350.0, 300.0, 1500.0)
        repo.obtenerProgresoMetasResult = ApiResult.Success(progreso, ApiCode.GOALS_PROGRESS_OK)
        vm.cargarResumen()
        assertEquals(progreso, vm.progresoMetas.value)

        repo.obtenerProgresoMetasResult = apiError(ApiCode.META_NO_ENCONTRADA)
        vm.cargarResumen()

        assertNull(vm.progresoMetas.value)
        assertEquals(2, repo.obtenerProgresoMetasCalls)
    }

    @Test
    fun `error de cuota distinto de meta inexistente muestra error al calcular con el codigo`() {
        repo.obtenerCuotaDiariaResult = apiError(ApiCode.UNKNOWN)

        vm.cargarResumen()

        assertEquals("Error al calcular", vm.cuotaActual.value)
        assertEquals("Error del servidor: UNKNOWN", vm.mensajeUI.value)
    }

    @Test
    fun `error en progreso o resumen semanal sin meta inexistente avisa por mensajeUI`() {
        repo.obtenerProgresoMetasResult = apiError(ApiCode.VALIDATION_ERROR)
        vm.cargarResumen()
        assertEquals("Datos inválidos. Revisá los campos.", vm.mensajeUI.value)

        vm.limpiarMensaje()
        assertNull(vm.mensajeUI.value)

        repo.obtenerProgresoMetasResult = ApiResult.Success(
            ProgresoMetas(0.0, 0.0, 0.0, 0.0, 0.0, 0.0), ApiCode.GOALS_PROGRESS_OK
        )
        val semana = listOf(ResumenSemanalDia("LUN", 40.0, 15.0))
        repo.obtenerResumenSemanalResult = ApiResult.Success(semana, ApiCode.WEEKLY_SUMMARY_OK)
        vm.cargarResumen()
        assertEquals(semana, vm.resumenSemanal.value)

        repo.obtenerResumenSemanalResult = apiError(ApiCode.UNAUTHORIZED)
        vm.cargarResumen()

        assertEquals("Sesión expirada. Volvé a iniciar sesión.", vm.mensajeUI.value)
        assertEquals(semana, vm.resumenSemanal.value)
    }

    @Test
    fun `error al cargar ingreso de hoy no publica mensaje`() {
        for (codigo in listOf(ApiCode.UNKNOWN, ApiCode.META_NO_ENCONTRADA)) {
            repo.obtenerHoyResult = ApiResult.Success(5.0, ApiCode.TODAY_INCOME_OK)
            vm.cargarResumen()
            assertEquals("5.00", vm.ingresoHoy.value)

            repo.obtenerHoyResult = apiError(codigo)
            vm.cargarResumen()
            assertEquals("codigo $codigo", "--", vm.ingresoHoy.value)
        }

        repo.obtenerHoyResult = apiError(ApiCode.UNAUTHORIZED)
        vm.cargarResumen()

        assertNull(vm.mensajeUI.value)
    }

    @Test
    fun `registrarTransaccion rechaza montos no numericos, cero o negativos sin llamar al repositorio`() {
        // No comma case: "10,5" becomes valid once this screen parses through toMontoPositivo
        // (#12, #17), and MontoTest pins it there.
        for (invalido in listOf("", "abc", "0", "-3")) {
            vm.limpiarMensaje()
            vm.registrarTransaccion(invalido, "INGRESO", null, null)
            assertEquals("monto '$invalido'", "Ingresá un monto válido (mayor a 0)", vm.mensajeUI.value)
        }

        assertTrue(repo.registrarTransaccionCalls.isEmpty())
        assertEquals(0, repo.obtenerHoyCalls)
    }

    @Test
    fun `registrarTransaccion exitosa envia datos normalizados, avisa segun tipo y refresca`() {
        vm.registrarTransaccion(" 25.5 ", "EGRESO", "   ", 4L)

        assertEquals(
            listOf(FakeFinanzasRepository.RegistrarTransaccionCall(25.5, "EGRESO", null, null, 4L)),
            repo.registrarTransaccionCalls
        )
        assertEquals("Egreso registrado.", vm.mensajeUI.value)
        assertEquals(1, repo.obtenerHoyCalls)

        vm.registrarTransaccion("10", "INGRESO", "Venta", null)

        assertEquals("Venta", repo.registrarTransaccionCalls.last().descripcion)
        assertEquals("¡Ingreso registrado!", vm.mensajeUI.value)
        assertEquals(2, repo.obtenerHoyCalls)
    }

    @Test
    fun `registrarIngreso delega como INGRESO sin descripcion ni categoria`() {
        vm.registrarIngreso("40")

        assertEquals(
            listOf(FakeFinanzasRepository.RegistrarTransaccionCall(40.0, "INGRESO", null, null, null)),
            repo.registrarTransaccionCalls
        )
        assertEquals("¡Ingreso registrado!", vm.mensajeUI.value)
    }

    @Test
    fun `registrarTransaccion con error mapea el mensaje y no refresca`() {
        repo.registrarTransaccionResult = apiError(ApiCode.VALIDATION_ERROR)

        vm.registrarTransaccion("10", "INGRESO", null, null)

        assertEquals(1, repo.registrarTransaccionCalls.size)
        assertEquals("Datos inválidos. Revisá los campos.", vm.mensajeUI.value)
        assertEquals(0, repo.obtenerHoyCalls)
    }

    @Test
    fun `fijarMeta rechaza monto invalido sin llamar al repositorio`() {
        for (invalido in listOf("", "abc", "0", "-100")) {
            vm.limpiarMensaje()
            vm.fijarMeta(invalido, listOf(1, 2))
            assertEquals("monto '$invalido'", "Ingresá un monto objetivo válido", vm.mensajeUI.value)
        }

        assertTrue(repo.fijarMetaCalls.isEmpty())
    }

    @Test
    fun `fijarMeta sin dias laborales no llama al repositorio`() {
        vm.fijarMeta("1000", emptyList())

        assertEquals("Seleccioná al menos un día laboral", vm.mensajeUI.value)
        assertTrue(repo.fijarMetaCalls.isEmpty())
    }

    @Test
    fun `fijarMeta exitosa envia monto y dias, avisa y refresca el dashboard`() {
        vm.fijarMeta(" 1500.5 ", listOf(1, 2, 3))

        assertEquals(
            listOf(FakeFinanzasRepository.FijarMetaCall(1500.5, listOf(1, 2, 3))),
            repo.fijarMetaCalls
        )
        assertEquals("¡Meta fijada correctamente!", vm.mensajeUI.value)
        assertEquals(1, repo.obtenerCuotaDiariaCalls)
        assertEquals(1, repo.obtenerProgresoMetasCalls)
    }

    @Test
    fun `fijarMeta con error mapea el mensaje y no refresca`() {
        repo.fijarMetaResult = apiError(ApiCode.UNAUTHORIZED)

        vm.fijarMeta("1000", listOf(1))

        assertEquals(1, repo.fijarMetaCalls.size)
        assertEquals("Sesión expirada. Volvé a iniciar sesión.", vm.mensajeUI.value)
        assertEquals(0, repo.obtenerCuotaDiariaCalls)
    }

    @Test
    fun `cargarCategorias publica la lista y un error posterior la conserva`() {
        val categorias = listOf(Categoria(1, "Comida", "EGRESO"))
        repo.listarCategoriasResult = ApiResult.Success(categorias, ApiCode.CATEGORIES_OK)
        vm.cargarCategorias()
        assertEquals(categorias, vm.categorias.value)

        repo.listarCategoriasResult = apiError(ApiCode.UNAUTHORIZED)
        vm.cargarCategorias()

        assertEquals(2, repo.listarCategoriasCalls)
        assertEquals(categorias, vm.categorias.value)
        assertNull(vm.mensajeUI.value)
    }

    @Test
    fun `cargarCuotaDiaria delega en cargarResumen`() {
        vm.cargarCuotaDiaria()

        assertEquals(1, repo.obtenerHoyCalls)
        assertEquals(1, repo.obtenerResumenSemanalCalls)
    }
}
