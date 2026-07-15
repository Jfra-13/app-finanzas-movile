package com.example.finanzas_independientes_app.data.mapper

import com.example.finanzas_independientes_app.data.remote.dto.PaginatedTransaccionDTO
import com.example.finanzas_independientes_app.data.remote.dto.TransaccionDTO
import com.example.finanzas_independientes_app.domain.model.PaginatedTransacciones
import com.example.finanzas_independientes_app.domain.model.Transaccion

fun TransaccionDTO.toDomain(): Transaccion = Transaccion(
    id = id,
    monto = monto,
    tipo = tipo,
    descripcion = descripcion,
    fecha = fecha,
    categoriaId = categoriaId,
    categoriaNombre = categoriaNombre,
    usuarioId = usuarioId
)

fun PaginatedTransaccionDTO.toDomain(): PaginatedTransacciones = PaginatedTransacciones(
    content = content.map { it.toDomain() },
    totalElements = totalElements,
    totalPages = totalPages,
    number = number,
    size = size,
    first = first,
    last = last
)
