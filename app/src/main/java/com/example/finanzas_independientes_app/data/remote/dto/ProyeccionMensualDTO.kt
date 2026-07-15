package com.example.finanzas_independientes_app.data.remote.dto

import com.google.gson.annotations.SerializedName

/** GET /finanzas/proyeccion-mensual: linear run-rate projection for the current month. */
data class ProyeccionMensualDTO(
    @SerializedName("periodo") val periodo: String,
    @SerializedName("diasTranscurridos") val diasTranscurridos: Int,
    @SerializedName("diasDelMes") val diasDelMes: Int,
    @SerializedName("diasHabilesRestantes") val diasHabilesRestantes: Int,
    @SerializedName("ingresoActual") val ingresoActual: Double,
    @SerializedName("egresoActual") val egresoActual: Double,
    @SerializedName("utilidadActual") val utilidadActual: Double,
    @SerializedName("ingresoProyectado") val ingresoProyectado: Double,
    @SerializedName("egresoProyectado") val egresoProyectado: Double,
    @SerializedName("utilidadProyectada") val utilidadProyectada: Double,
    @SerializedName("metaMensual") val metaMensual: Double,
    @SerializedName("brechaProyectada") val brechaProyectada: Double,
    @SerializedName("enCamino") val enCamino: Boolean
)
