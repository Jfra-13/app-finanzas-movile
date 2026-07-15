package com.example.finanzas_independientes_app.core.network

/**
 * Maps a domain [AppError] to a user-facing message. UI copy is Spanish to
 * stay consistent with the rest of the app. Branches on [ApiCode], never on
 * the backend message.
 */
fun AppError.toUserMessage(): String = when (this) {
    is AppError.Network -> "Sin conexión. Revisá tu internet e intentá de nuevo."
    is AppError.Unexpected -> "Ocurrió un error inesperado. Intentá de nuevo."
    is AppError.Api -> when (code) {
        ApiCode.VALIDATION_ERROR ->
            fieldErrors.firstOrNull()?.message ?: "Revisá los datos ingresados."
        ApiCode.UNAUTHORIZED -> "Correo o contraseña incorrectos."
        ApiCode.CREDENCIALES_INVALIDAS -> "Contraseña incorrecta."
        ApiCode.REFRESH_TOKEN_INVALIDO -> "Tu sesión expiró. Iniciá sesión de nuevo."
        ApiCode.EMAIL_DUPLICADO -> "Ese correo ya está registrado."
        ApiCode.OTP_INVALIDO -> "El código ingresado no es válido."
        ApiCode.OTP_EXPIRADO -> "El código expiró. Pedí uno nuevo."
        ApiCode.OTP_BLOQUEADO -> "Demasiados intentos. Esperá unos minutos."
        ApiCode.ACCESO_DENEGADO, ApiCode.FORBIDDEN -> "No tenés permiso para esta acción."
        ApiCode.META_NO_ENCONTRADA -> "Todavía no fijaste una meta este mes."
        ApiCode.CATEGORIA_NO_ENCONTRADA -> "La categoría seleccionada no existe."
        ApiCode.PRESUPUESTO_NO_ENCONTRADO -> "No se encontró el presupuesto."
        ApiCode.TRANSACCION_NO_ENCONTRADA -> "No se encontró el movimiento."
        ApiCode.USUARIO_NO_ENCONTRADO -> "Usuario no encontrado."
        ApiCode.PARAMETRO_INVALIDO -> "Hay un parámetro inválido en la consulta."
        ApiCode.RANGO_FECHAS_INVALIDO -> "El rango de fechas no es válido."
        else -> message ?: "No se pudo completar la operación."
    }
}
