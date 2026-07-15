package com.example.finanzas_independientes_app.data.mapper

import com.example.finanzas_independientes_app.data.remote.dto.MetaHistorialItemDTO
import com.example.finanzas_independientes_app.domain.model.MetaHistorialItem

fun MetaHistorialItemDTO.toDomain(): MetaHistorialItem = MetaHistorialItem(
    periodo = periodo,
    metaMensual = metaMensual,
    utilidadReal = utilidadReal,
    cumplida = cumplida
)
