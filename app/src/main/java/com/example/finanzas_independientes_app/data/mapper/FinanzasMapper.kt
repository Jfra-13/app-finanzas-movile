package com.example.finanzas_independientes_app.data.mapper

import com.example.finanzas_independientes_app.data.remote.dto.IngresoDiaSemanaItemDTO
import com.example.finanzas_independientes_app.data.remote.dto.ProgresoMetasDTO
import com.example.finanzas_independientes_app.data.remote.dto.ResumenDiarioItemDTO
import com.example.finanzas_independientes_app.data.remote.dto.ResumenSemanalItemDTO
import com.example.finanzas_independientes_app.data.remote.dto.SaludFinancieraItemDTO
import com.example.finanzas_independientes_app.data.remote.dto.TendenciaDTO
import com.example.finanzas_independientes_app.domain.model.IngresoDiaSemana
import com.example.finanzas_independientes_app.domain.model.ProgresoMetas
import com.example.finanzas_independientes_app.domain.model.ResumenDiarioDia
import com.example.finanzas_independientes_app.domain.model.ResumenSemanalDia
import com.example.finanzas_independientes_app.domain.model.SaludFinancieraItem
import com.example.finanzas_independientes_app.domain.model.Tendencia

fun ResumenSemanalItemDTO.toDomain(): ResumenSemanalDia = ResumenSemanalDia(
    dia = dia,
    ingresos = ingresos,
    egresos = egresos
)

fun ResumenDiarioItemDTO.toDomain(): ResumenDiarioDia = ResumenDiarioDia(
    fecha = fecha,
    ingresos = ingresos,
    egresos = egresos
)

fun ProgresoMetasDTO.toDomain(): ProgresoMetas = ProgresoMetas(
    ingresoDiario = ingresoDiario,
    metaDiaria = metaDiaria,
    ingresoSemanal = ingresoSemanal,
    metaSemanal = metaSemanal,
    ingresoMensual = ingresoMensual,
    metaMensual = metaMensual
)

fun SaludFinancieraItemDTO.toDomain(): SaludFinancieraItem = SaludFinancieraItem(
    tipo = tipo,
    code = code,
    mensaje = mensaje
)

fun TendenciaDTO.toDomain(): Tendencia = Tendencia(
    periodos = periodos,
    ingresos = ingresos,
    egresos = egresos
)

fun IngresoDiaSemanaItemDTO.toDomain(): IngresoDiaSemana = IngresoDiaSemana(
    dia = dia,
    ingresos = ingresos
)
