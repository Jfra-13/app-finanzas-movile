package com.example.finanzas_independientes_app.presentation.common

/**
 * Parses a user-typed amount. Returns null unless it is a finite number above
 * zero: `toDoubleOrNull()` alone accepts "NaN" and "Infinity", and `NaN <= 0`
 * is false, so a plain sign check lets them through.
 */
fun String.toMontoPositivo(): Double? =
    trim().toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 }
