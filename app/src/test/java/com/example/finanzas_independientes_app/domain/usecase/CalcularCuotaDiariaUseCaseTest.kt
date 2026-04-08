package com.example.finanzas_independientes_app.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class CalcularCuotaDiariaUseCaseTest {

    // Instanciamos la clase que vamos a probar (saldrá en rojo porque aún no existe)
    private val useCase = CalcularCuotaDiariaUseCase()

    @Test
    fun `dado que faltan dias y dinero, debe retornar la division exacta`() {
        // 1. Arrange (Preparar el escenario)
        val metaMensual = 3000.0
        val utilidadActual = 1500.0
        val diasRestantes = 15

        // 2. Act (Ejecutar la acción)
        val resultado = useCase(metaMensual, utilidadActual, diasRestantes)

        // 3. Assert (Verificar el resultado)
        // Se espera 100.0 (1500 faltantes / 15 días)
        assertEquals(100.0, resultado, 0.0)
    }

    @Test
    fun `dado que la utilidad supero la meta, debe retornar 0`() {
        val resultado = useCase(3000.0, 3100.0, 10)
        assertEquals(0.0, resultado, 0.0)
    }

    @Test
    fun `dado que es el ultimo dia, debe retornar el saldo faltante total`() {
        val resultado = useCase(3000.0, 2500.0, 0)
        assertEquals(500.0, resultado, 0.0)
    }
}