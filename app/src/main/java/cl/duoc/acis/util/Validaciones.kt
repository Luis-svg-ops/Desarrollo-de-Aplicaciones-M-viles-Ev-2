package cl.duoc.acis.util

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
