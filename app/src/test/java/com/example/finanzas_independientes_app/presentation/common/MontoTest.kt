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
