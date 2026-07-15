package com.example.finanzas_independientes_app.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProyectarTendenciaUseCaseTest {

    private val useCase = ProyectarTendenciaUseCase()

    @Test
    fun `serie perfectamente lineal proyecta la continuacion exacta`() {
        // y = 2x + 1  -> 1, 3, 5, 7
        val serie = listOf(1.0, 3.0, 5.0, 7.0)

        val resultado = useCase(serie, pasos = 2)

        // x = 4, 5 -> 9, 11
        assertEquals(2, resultado.size)
        assertEquals(9.0, resultado[0], 1e-6)
        assertEquals(11.0, resultado[1], 1e-6)
    }

    @Test
    fun `serie constante proyecta el mismo valor`() {
        val resultado = useCase(listOf(500.0, 500.0, 500.0), pasos = 1)
        assertEquals(500.0, resultado[0], 1e-6)
    }

    @Test
    fun `tendencia a la baja proyecta valores decrecientes`() {
        val serie = listOf(100.0, 80.0, 55.0, 45.0)
        val resultado = useCase(serie, pasos = 1)
        assertTrue("expected next point below last observed", resultado[0] < serie.last())
    }

    @Test
    fun `historia insuficiente retorna vacio`() {
        assertTrue(useCase(listOf(10.0), pasos = 3).isEmpty())
        assertTrue(useCase(emptyList(), pasos = 3).isEmpty())
    }

    @Test
    fun `pasos no positivos retorna vacio`() {
        assertTrue(useCase(listOf(1.0, 2.0, 3.0), pasos = 0).isEmpty())
    }
}
