package com.example.finanzas_independientes_app.domain.usecase

import com.example.finanzas_independientes_app.domain.model.SaludFinancieraItem
import org.junit.Assert.assertEquals
import org.junit.Test

class OrdenarSenalesSaludUseCaseTest {

    private val useCase = OrdenarSenalesSaludUseCase()

    private fun senal(code: String, severidad: String) = SaludFinancieraItem(
        tipo = "ALERTA",
        code = code,
        severidad = severidad,
        mensaje = code,
        categoriaId = null
    )

    @Test
    fun `ordena ALTA antes que MEDIA antes que BAJA`() {
        val entrada = listOf(
            senal("a", "BAJA"),
            senal("b", "ALTA"),
            senal("c", "MEDIA")
        )
        val salida = useCase(entrada)
        assertEquals(listOf("b", "c", "a"), salida.map { it.code })
    }

    @Test
    fun `mantiene el orden del server entre senales de igual severidad`() {
        val entrada = listOf(
            senal("primero", "ALTA"),
            senal("segundo", "ALTA")
        )
        val salida = useCase(entrada)
        assertEquals(listOf("primero", "segundo"), salida.map { it.code })
    }

    @Test
    fun `severidad desconocida queda al final`() {
        val entrada = listOf(
            senal("rara", "CRITICA"),
            senal("normal", "MEDIA")
        )
        val salida = useCase(entrada)
        assertEquals(listOf("normal", "rara"), salida.map { it.code })
    }

    @Test
    fun `lista vacia retorna vacia`() {
        assertEquals(emptyList<SaludFinancieraItem>(), useCase(emptyList()))
    }
}
