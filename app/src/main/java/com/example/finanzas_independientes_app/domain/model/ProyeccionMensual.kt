package com.example.finanzas_independientes_app.domain.model

/** Linear run-rate projection for the current month. */
data class ProyeccionMensual(
    val periodo: String,
    val diasTranscurridos: Int,
    val diasDelMes: Int,
    val diasHabilesRestantes: Int,
    val ingresoActual: Double,
    val egresoActual: Double,
    val utilidadActual: Double,
    val ingresoProyectado: Double,
    val egresoProyectado: Double,
    val utilidadProyectada: Double,
    val metaMensual: Double,
    val brechaProyectada: Double,
    val enCamino: Boolean
)
