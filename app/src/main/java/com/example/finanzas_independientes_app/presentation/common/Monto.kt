package com.example.finanzas_independientes_app.presentation.common

/**
 * Parses a user-typed amount. Returns null unless it is a finite number above
 * zero: `toDoubleOrNull()` alone accepts "NaN" and "Infinity", and `NaN <= 0`
 * is false, so a plain sign check lets them through.
 *
 * A comma is read as the decimal separator ("10,5" = 10.5), as Spanish keyboards
 * type it. There is no thousands separator: "1,500" is 1.5, and "1.500,50" is rejected.
 */
fun String.toMontoPositivo(): Double? =
    trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 }
