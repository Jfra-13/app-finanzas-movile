package com.example.finanzas_independientes_app.presentation.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Every amount form funnels through [toMontoPositivo]; non-finite input must never reach the API. */
class MontoTest {

    @Test
    fun `un monto positivo se parsea recortando espacios`() {
        assertEquals(150.5, " 150.5 ".toMontoPositivo()!!, 0.0)
    }

    @Test
    fun `la coma se acepta como separador decimal`() {
        assertEquals(10.5, "10,5".toMontoPositivo()!!, 0.0)
        assertEquals(1.5, "1,500".toMontoPositivo()!!, 0.0) // decimal, never a thousands separator
    }

    @Test
    fun `mas de un separador se rechaza`() {
        assertNull("1.500,50".toMontoPositivo())
        assertNull("1,2,3".toMontoPositivo())
    }

    @Test
    fun `NaN e Infinity se rechazan`() {
        assertNull("NaN".toMontoPositivo())
        assertNull("Infinity".toMontoPositivo())
        assertNull("-Infinity".toMontoPositivo())
        assertNull("1e400".toMontoPositivo()) // overflows to Infinity
    }

    @Test
    fun `cero, negativos y texto se rechazan`() {
        assertNull("0".toMontoPositivo())
        assertNull("-5".toMontoPositivo())
        assertNull("abc".toMontoPositivo())
        assertNull("".toMontoPositivo())
    }
}
