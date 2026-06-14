package com.example.finanzas_independientes_app.data.mapper

import com.example.finanzas_independientes_app.data.remote.dto.AuthData
import com.example.finanzas_independientes_app.domain.model.AuthSession
import com.example.finanzas_independientes_app.domain.model.Usuario

fun AuthData.toUsuario(): Usuario = Usuario(
    usuarioId = usuarioId,
    nombre = nombre,
    email = email,
    tipoNegocio = tipoNegocio
)

fun AuthData.toAuthSession(): AuthSession = AuthSession(
    token = token,
    refreshToken = refreshToken,
    usuario = toUsuario()
)
