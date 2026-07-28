package com.example.finanzas_independientes_app.data.mapper

import com.example.finanzas_independientes_app.data.remote.dto.AuthData
import com.example.finanzas_independientes_app.data.remote.dto.PaginatedTransaccionDTO
import com.example.finanzas_independientes_app.data.remote.dto.TransaccionDTO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Only the mappers that actually restructure data are covered here. The
 * field-for-field ones would make the test a copy of the mapper, so they are
 * left out on purpose.
 */
class MapperTest {

    @Test
    fun `toAuthSession anida el usuario y propaga cuentaReactivada`() {
        val auth = AuthData(
            token = "acceso",
            refreshToken = "refresco",
            usuarioId = 42,
            nombre = "Ana",
            email = "ana@test.com",
            tipoNegocio = "SERVICIOS",
            cuentaReactivada = true
        )

        val session = auth.toAuthSession()

        assertEquals("acceso", session.token)
        assertEquals("refresco", session.refreshToken)
        assertEquals(42L, session.usuario.usuarioId)
        assertEquals("SERVICIOS", session.usuario.tipoNegocio)
        // Drives the "your account was reactivated" notice after login.
        assertTrue(session.cuentaReactivada)
    }

    @Test
    fun `cuentaReactivada es false cuando el server no la manda`() {
        val auth = AuthData("t", "r", 1, "Ana", "ana@test.com", null)

        assertFalse(auth.toAuthSession().cuentaReactivada)
    }

    @Test
    fun `la pagina mapea su contenido y conserva los metadatos`() {
        val page = PaginatedTransaccionDTO(
            content = listOf(
                TransaccionDTO(1, 100.0, "INGRESO", "venta", "2026-07-01", 3, "Ventas", 42),
                TransaccionDTO(2, 50.0, "EGRESO", "insumos", "2026-07-02", null, null, 42)
            ),
            totalElements = 2,
            totalPages = 1,
            number = 0,
            size = 20,
            first = true,
            last = true
        )

        val domain = page.toDomain()

        assertEquals(2, domain.content.size)
        assertEquals("Ventas", domain.content[0].categoriaNombre)
        // Uncategorized rows must survive the mapping with a null category.
        assertEquals(null, domain.content[1].categoriaId)
        assertEquals(2L, domain.totalElements)
        assertTrue(domain.first)
        assertTrue(domain.last)
    }
}
