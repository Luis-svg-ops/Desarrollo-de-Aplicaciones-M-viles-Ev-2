package cl.duoc.acis.model

/**
 * Acciones que cambian el estado de una OT (Excel: hoja "Estados OT").
 * Cada acción dice desde qué estados se puede hacer, a qué estado lleva,
 * qué rol la hace y qué motivos se pueden elegir (lista vacía = no pide motivo).
 */
enum class AccionOT(
    val texto: String,
    val desde: Set<EstadoOT>,
    val hacia: EstadoOT,
    val rol: Rol,
    val motivos: List<String> = emptyList(),
    // true = se hace guardando el formulario de edición, no con un botón del detalle
    val enFormulario: Boolean = false
) {
    ACEPTAR("Aceptar OT", setOf(EstadoOT.ASIGNADA), EstadoOT.ACEPTADA, Rol.TECNICO),

    RECHAZAR(
        "Rechazar", setOf(EstadoOT.ASIGNADA), EstadoOT.RECHAZADA, Rol.TECNICO,
        listOf("Fuera de mi zona", "Sin herramientas o repuestos", "Superpuesta con otra visita")
    ),

    // Hito 6: el check-in con GPS hace este cambio
    CHECK_IN("Hacer check-in", setOf(EstadoOT.ACEPTADA), EstadoOT.EN_EJECUCION, Rol.TECNICO),

    VISITA_FALLIDA(
        "Registrar visita fallida", setOf(EstadoOT.ACEPTADA), EstadoOT.VISITA_FALLIDA, Rol.TECNICO,
        listOf("Local cerrado", "Sin acceso a la azotea", "Encargado ausente")
    ),

    // Hito 7: el diagnóstico con check-out hace este cambio
    CERRAR("Registrar diagnóstico y cerrar", setOf(EstadoOT.EN_EJECUCION), EstadoOT.CERRADA, Rol.TECNICO),

    // Revisión de la jefatura: revisa foto, diagnóstico, GPS y duración
    CONFORMIDAD("Dar conformidad", setOf(EstadoOT.CERRADA), EstadoOT.CONFORME, Rol.JEFE),

    // Si el trabajo no está bien, vuelve al técnico para que lo corrija y cierre de nuevo
    DISCONFORMIDAD(
        "Disconformidad", setOf(EstadoOT.CERRADA), EstadoOT.EN_EJECUCION, Rol.JEFE,
        listOf("Foto poco clara", "Trabajo incompleto", "Falta información en el diagnóstico")
    ),

    POSTERGAR(
        "Postergar", setOf(EstadoOT.ASIGNADA, EstadoOT.ACEPTADA), EstadoOT.POSTERGADA, Rol.JEFE,
        listOf("Lluvia o viento fuerte", "Solicitud del cliente", "Falta de repuesto")
    ),

    // Se hace desde el formulario de edición, al elegir técnico o una nueva fecha
    ASIGNAR(
        "Asignar o reprogramar",
        setOf(EstadoOT.CREADA, EstadoOT.ASIGNADA, EstadoOT.ACEPTADA, EstadoOT.RECHAZADA,
            EstadoOT.VISITA_FALLIDA, EstadoOT.POSTERGADA),
        EstadoOT.ASIGNADA, Rol.JEFE, enFormulario = true
    ),

    // También desde el formulario: el Jefe deja la OT sin técnico
    QUITAR_TECNICO(
        "Quitar técnico",
        setOf(EstadoOT.ASIGNADA, EstadoOT.ACEPTADA, EstadoOT.RECHAZADA,
            EstadoOT.VISITA_FALLIDA, EstadoOT.POSTERGADA),
        EstadoOT.CREADA, Rol.JEFE, enFormulario = true
    ),

    CANCELAR(
        "Cancelar OT",
        setOf(EstadoOT.CREADA, EstadoOT.ASIGNADA, EstadoOT.ACEPTADA, EstadoOT.RECHAZADA,
            EstadoOT.VISITA_FALLIDA, EstadoOT.POSTERGADA),
        EstadoOT.CANCELADA, Rol.JEFE,
        listOf("El cliente ya no la necesita", "OT duplicada", "Error de registro")
    );

    val pideMotivo: Boolean get() = motivos.isNotEmpty()
}

/**
 * Máquina de estados de la OT: el único lugar que decide qué cambios de estado están permitidos.
 * El repositorio la consulta antes de guardar y la pantalla la usa para saber qué botones mostrar,
 * así nunca aparece un botón para un cambio que la lógica no permite.
 */
object MaquinaEstadosOT {

    /** Estados finales: de aquí la OT ya no sale. */
    val estadosFinales = setOf(EstadoOT.CONFORME, EstadoOT.CANCELADA)

    /** A qué estados puede pasar una OT desde el estado dado (la "tabla de transiciones"). */
    fun destinos(estado: EstadoOT): Set<EstadoOT> =
        AccionOT.entries.filter { estado in it.desde }.map { it.hacia }.toSet()

    /** Acciones que un rol puede hacer sobre una OT en este estado (para mostrar los botones). */
    fun accionesDisponibles(estado: EstadoOT, rol: Rol): List<AccionOT> =
        AccionOT.entries.filter { it.rol == rol && estado in it.desde }

    /**
     * Revisa si la acción se puede hacer. Devuelve el mensaje de error o null si está permitida
     * (mismo estilo que las funciones de util/Validaciones).
     * - tecnicoDeLaOt / usuarioId: un técnico solo puede tocar las OT que tiene asignadas.
     */
    fun validar(
        accion: AccionOT,
        estado: EstadoOT,
        rol: Rol,
        motivo: String? = null,
        tecnicoDeLaOt: Long? = null,
        usuarioId: Long? = null
    ): String? = when {
        rol != accion.rol -> "Tu rol no permite esta acción"
        rol == Rol.TECNICO && tecnicoDeLaOt != usuarioId -> "Esta OT no está asignada a ti"
        estado !in accion.desde -> "Acción no permitida: la OT está ${estado.texto.lowercase()}"
        accion.pideMotivo && motivo.isNullOrBlank() -> "Selecciona un motivo"
        accion.pideMotivo && motivo !in accion.motivos -> "Motivo no válido"
        else -> null
    }

    /** Estado de una OT recién creada: Asignada si ya tiene técnico; si no, Creada. */
    fun estadoInicial(tecnicoId: Long?): EstadoOT =
        if (tecnicoId == null) EstadoOT.CREADA else EstadoOT.ASIGNADA

    /** El Jefe puede editar la OT mientras no esté en ejecución ni terminada. */
    fun sePuedeEditar(estado: EstadoOT): Boolean =
        estado != EstadoOT.EN_EJECUCION && estado != EstadoOT.CERRADA && estado !in estadosFinales

    /**
     * Estado en que queda una OT al guardar el formulario de edición:
     * - sin técnico: vuelve a Creada;
     * - con técnico nuevo, o si estaba sin asignar o detenida: (re)asignada;
     * - si solo se cambian otros datos: mantiene su estado.
     */
    fun estadoAlEditar(estado: EstadoOT, tecnicoAnterior: Long?, tecnicoNuevo: Long?): EstadoOT = when {
        tecnicoNuevo == null -> EstadoOT.CREADA
        tecnicoNuevo != tecnicoAnterior || estado != EstadoOT.ASIGNADA && estado != EstadoOT.ACEPTADA -> EstadoOT.ASIGNADA
        else -> estado
    }
}
