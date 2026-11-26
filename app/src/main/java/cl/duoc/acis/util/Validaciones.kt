package cl.duoc.acis.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

// Validaciones reutilizables (mismo estilo que ValidarRut del ejemplo del curso).
// Cada función devuelve el mensaje de error, o null si el valor es válido.
// No dependen de Android, así que se pueden probar con tests simples.

private val FORMATO_EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

fun validarEmail(email: String): String? = when {
    email.isBlank() -> "Ingresa tu correo"
    !FORMATO_EMAIL.matches(email.trim()) -> "Formato de correo no válido (ej.: nombre@acis.cl)"
    else -> null
}

/** Nombre completo: entre 3 y 60 caracteres (igual que ms-auth). */
fun validarNombre(nombre: String): String? = when {
    nombre.isBlank() -> "Ingresa tu nombre"
    nombre.trim().length < 3 -> "Mínimo 3 caracteres"
    nombre.trim().length > 60 -> "Máximo 60 caracteres"
    else -> null
}

private val FORMATO_TELEFONO = Regex("""^\+?[0-9 ]{8,16}$""")

/** Teléfono chileno: "+56 9 1234 5678" (mismo formato que valida el servidor). */
fun validarTelefono(telefono: String): String? = when {
    telefono.isBlank() -> "Ingresa un teléfono"
    !FORMATO_TELEFONO.matches(telefono.trim()) -> "Teléfono no válido (ej.: +56 9 1234 5678)"
    else -> null
}

/** Código del local que ACIS le entrega al cliente (ej.: MALL-02). */
fun validarCodigoLocal(codigo: String): String? = when {
    codigo.isBlank() -> "Ingresa el código de tu local"
    !Regex("^[A-Za-z0-9-]{4,20}$").matches(codigo.trim()) -> "Código no válido (ej.: MALL-02)"
    else -> null
}

/** Código de recuperación: exactamente 6 dígitos. */
fun validarCodigoRecuperacion(codigo: String): String? = when {
    codigo.isBlank() -> "Ingresa el código que te llegó"
    !Regex("^[0-9]{6}$").matches(codigo) -> "El código tiene 6 dígitos"
    else -> null
}

/** La confirmación tiene que ser igual a la contraseña nueva. */
fun validarConfirmacion(clave: String, confirmacion: String): String? = when {
    confirmacion.isEmpty() -> "Repite la contraseña"
    confirmacion != clave -> "Las contraseñas no coinciden"
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

/** Texto obligatorio corto (marca, modelo): no vacío y hasta 40 caracteres. */
fun validarObligatorio(texto: String, mensaje: String): String? = when {
    texto.isBlank() -> mensaje
    texto.length > 40 -> "Máximo 40 caracteres"
    else -> null
}

/** Última mantención: opcional, pero no puede ser una fecha futura. */
fun validarFechaNoFutura(fecha: LocalDate?, hoy: LocalDate = LocalDate.now()): String? =
    if (fecha != null && fecha.isAfter(hoy)) "La fecha no puede ser futura" else null

/** Si la OT es para hoy, la hora no puede haber pasado (no se programa una visita en el pasado). */
fun validarHoraProgramada(fecha: LocalDate?, hora: LocalTime, ahora: LocalDateTime = LocalDateTime.now()): String? =
    if (fecha == ahora.toLocalDate() && hora.isBefore(ahora.toLocalTime().withSecond(0).withNano(0))) "Esa hora ya pasó" else null
