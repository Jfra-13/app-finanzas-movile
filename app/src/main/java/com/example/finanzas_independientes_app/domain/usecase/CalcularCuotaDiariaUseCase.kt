package com.example.finanzas_independientes_app.domain.usecase

class CalcularCuotaDiariaUseCase {

    operator fun invoke(metaMensual: Double, utilidadActual: Double, diasRestantes: Int): Double {
        if (utilidadActual >= metaMensual) {
            return 0.0 // Meta ya alcanzada o superada
        }

        val faltante = metaMensual - utilidadActual

        if (diasRestantes <= 0) {
            return faltante // Último día, debe cubrir todo lo que falta
        }

        return faltante / diasRestantes
    }
}