package com.example.finanzas_independientes_app.data.mapper

import com.example.finanzas_independientes_app.data.remote.dto.MetaDTO
import com.example.finanzas_independientes_app.domain.model.Meta

fun MetaDTO.toDomain(): Meta = Meta(
    id = id,
    montoObjetivo = montoObjetivo,
    periodo = periodo,
    diasLaborables = diasLaborables,
    activa = activa
)
