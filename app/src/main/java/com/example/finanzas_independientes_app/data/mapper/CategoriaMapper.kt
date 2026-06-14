package com.example.finanzas_independientes_app.data.mapper

import com.example.finanzas_independientes_app.data.remote.dto.CategoriaDTO
import com.example.finanzas_independientes_app.domain.model.Categoria

fun CategoriaDTO.toDomain(): Categoria = Categoria(
    id = id,
    nombre = nombre,
    tipo = tipo
)
