package com.example.finanzas_independientes_app.data.mapper

import com.example.finanzas_independientes_app.data.remote.dto.ComparacionCategoriaItemDTO
import com.example.finanzas_independientes_app.data.remote.dto.ComparacionCategoriasDTO
import com.example.finanzas_independientes_app.data.remote.dto.IngresoDiaSemanaItemDTO
import com.example.finanzas_independientes_app.data.remote.dto.PeriodoDTO
import com.example.finanzas_independientes_app.data.remote.dto.PresupuestoDTO
import com.example.finanzas_independientes_app.data.remote.dto.ProgresoMetasDTO
import com.example.finanzas_independientes_app.data.remote.dto.ProyeccionMensualDTO
import com.example.finanzas_independientes_app.data.remote.dto.ResumenDiarioItemDTO
import com.example.finanzas_independientes_app.data.remote.dto.ResumenSemanalItemDTO
import com.example.finanzas_independientes_app.data.remote.dto.SaludFinancieraItemDTO
import com.example.finanzas_independientes_app.data.remote.dto.TendenciaDTO
import com.example.finanzas_independientes_app.domain.model.ComparacionCategoriaItem
import com.example.finanzas_independientes_app.domain.model.ComparacionCategorias
import com.example.finanzas_independientes_app.domain.model.IngresoDiaSemana
import com.example.finanzas_independientes_app.domain.model.Periodo
import com.example.finanzas_independientes_app.domain.model.Presupuesto
import com.example.finanzas_independientes_app.domain.model.ProgresoMetas
import com.example.finanzas_independientes_app.domain.model.ProyeccionMensual
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
    severidad = severidad,
    mensaje = mensaje,
    categoriaId = categoriaId
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

fun PresupuestoDTO.toDomain(): Presupuesto = Presupuesto(
    id = id,
    categoriaId = categoriaId,
    categoriaNombre = categoriaNombre,
    montoMensual = montoMensual,
    gastadoMes = gastadoMes,
    restante = restante,
    consumoPct = consumoPct,
    excedido = excedido
)

fun ProyeccionMensualDTO.toDomain(): ProyeccionMensual = ProyeccionMensual(
    periodo = periodo,
    diasTranscurridos = diasTranscurridos,
    diasDelMes = diasDelMes,
    diasHabilesRestantes = diasHabilesRestantes,
    ingresoActual = ingresoActual,
    egresoActual = egresoActual,
    utilidadActual = utilidadActual,
    ingresoProyectado = ingresoProyectado,
    egresoProyectado = egresoProyectado,
    utilidadProyectada = utilidadProyectada,
    metaMensual = metaMensual,
    brechaProyectada = brechaProyectada,
    enCamino = enCamino
)

private fun PeriodoDTO.toDomain(): Periodo = Periodo(desde = desde, hasta = hasta)

private fun ComparacionCategoriaItemDTO.toDomain(): ComparacionCategoriaItem = ComparacionCategoriaItem(
    categoria = categoria,
    actual = actual,
    anterior = anterior,
    deltaAbs = deltaAbs,
    deltaPct = deltaPct
)

fun ComparacionCategoriasDTO.toDomain(): ComparacionCategorias = ComparacionCategorias(
    periodoActual = periodoActual.toDomain(),
    periodoAnterior = periodoAnterior.toDomain(),
    categorias = categorias.map { it.toDomain() },
    totalActual = totalActual,
    totalAnterior = totalAnterior,
    totalDeltaPct = totalDeltaPct
)
