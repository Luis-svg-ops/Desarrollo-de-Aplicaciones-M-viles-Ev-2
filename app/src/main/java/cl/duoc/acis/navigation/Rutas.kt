package cl.duoc.acis.navigation

/** Rutas de navegación de la app (mismo estilo que el ejemplo del profe). */
sealed class Rutas(val route: String) {
    data object Login : Rutas("login")

    // Pantallas principales (con barra inferior)
    data object Ots : Rutas("ots")
    data object Locales : Rutas("locales")
    data object Perfil : Rutas("perfil")

    // Pantallas internas (con flecha atrás)
    data object DetalleOt : Rutas("ot/{otId}") {
        fun createRoute(otId: Long) = "ot/$otId"
    }
    data object OtForm : Rutas("ot_form?otId={otId}") {
        fun createRoute(otId: Long? = null) = if (otId == null) "ot_form" else "ot_form?otId=$otId"
    }
    data object Diagnostico : Rutas("diagnostico/{otId}") {
        fun createRoute(otId: Long) = "diagnostico/$otId"
    }
    data object Local : Rutas("local/{localId}") {
        fun createRoute(localId: Long) = "local/$localId"
    }
    data object EquipoForm : Rutas("equipo_form/{localId}?equipoId={equipoId}") {
        fun createRoute(localId: Long, equipoId: Long? = null) =
            if (equipoId == null) "equipo_form/$localId" else "equipo_form/$localId?equipoId=$equipoId"
    }

    companion object {
        /** Rutas que muestran la barra inferior. */
        val principales get() = setOf(Ots.route, Locales.route, Perfil.route)
    }
}
