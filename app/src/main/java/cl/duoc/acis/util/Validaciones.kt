package cl.duoc.acis.util

import java.time.LocalDate

// Validaciones reutilizables (mismo estilo que ValidarRut del ejemplo del curso).
// Cada función devuelve el mensaje de error, o null si el valor es válido.
// No dependen de Android, así que se pueden probar con tests simples.

private val FORMATO_EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

fun validarEmail(email: String): String? = when {
    email.isBlank() -> "Ingresa tu correo"
    !FORMATO_EMAIL.matches(email.trim()) -> "Formato de correo no válido (ej.: nombre@acis.cl)"
    else -> null
}

fun validarPassword(password: String): String? = when {
    password.isEmpty() -> "Ingresa tu contraseña"
    password.length < 6 -> "Debe tener al menos 6 caracteres"
    else -> null
}

/** Para listas desplegables obligatorias (local, equipo). */
fun validarSeleccion(valor: Any?, mensaje: String): String? = if (valor == null) mensaje else null

/** La fecha programada es obligatoria y no puede ser anterior a hoy. */
fun validarFechaProgramada(fecha: LocalDate?, hoy: LocalDate = LocalDate.now()): String? = when {
    fecha == null -> "Selecciona una fecha"
    fecha.isBefore(hoy) -> "La fecha no puede ser anterior a hoy"
    else -> null
}

/** Descripción del trabajo: entre 10 y 300 caracteres. */
fun validarDescripcion(texto: String): String? {
    val largo = texto.trim().length
    return when {
        largo < 10 -> "Mínimo 10 caracteres (llevas $largo)"
        texto.length > 300 -> "Máximo 300 caracteres"
        else -> null
    }
}

/** Texto obligatorio del cierre (diagnóstico, actividades): entre 10 y 500 caracteres. */
fun validarTextoCierre(texto: String, campo: String): String? {
    val largo = texto.trim().length
    return when {
        largo == 0 -> "Ingresa $campo"
        largo < 10 -> "Mínimo 10 caracteres (llevas $largo)"
        texto.length > 500 -> "Máximo 500 caracteres"
        else -> null
    }
}

/** La foto de evidencia es obligatoria para cerrar la OT. */
fun validarFoto(uri: String?): String? = if (uri.isNullOrBlank()) "Toma una foto del equipo" else null
