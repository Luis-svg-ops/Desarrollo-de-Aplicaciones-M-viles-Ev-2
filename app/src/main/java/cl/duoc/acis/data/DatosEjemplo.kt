package cl.duoc.acis.data

import cl.duoc.acis.model.EstadoOT
import cl.duoc.acis.model.Equipo
import cl.duoc.acis.model.Local
import cl.duoc.acis.model.OrdenTrabajo
import cl.duoc.acis.model.Prioridad
import cl.duoc.acis.model.Rol
import cl.duoc.acis.model.TipoEquipo
import cl.duoc.acis.model.TipoOT
import cl.duoc.acis.model.UbicacionEquipo
import cl.duoc.acis.model.Usuario
import java.time.LocalDate
import java.time.ZoneId

/**
 * Datos de ejemplo que se cargan al crear la base (los mismos del mockup y de NOTAS_ANDROID.md).
 * Las fechas son relativas a hoy, para que siempre haya OT "del día".
 */
object DatosEjemplo {

    // Fecha y hora en milisegundos: hoy + dias, a la hora indicada
    private fun fecha(dias: Long, hora: Int, minuto: Int = 0): Long =
        LocalDate.now().plusDays(dias).atTime(hora, minuto)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private val usuarios = listOf(
        Usuario(1, "Pedro Soto", "tecnico@acis.cl", "123456", Rol.TECNICO),
        Usuario(2, "Carolina Muñoz", "jefe@acis.cl", "123456", Rol.JEFE),
        Usuario(3, "Matías Rojas", "mrojas@acis.cl", "123456", Rol.TECNICO),
        Usuario(4, "Camila Fuentes", "cfuentes@acis.cl", "123456", Rol.TECNICO),
        Usuario(5, "Diego Araya", "daraya@acis.cl", "123456", Rol.TECNICO),
        // Cliente: Andrea Pino administra el Mall Costanera Oeste (local 2)
        Usuario(6, "Andrea Pino", "cliente@acis.cl", "123456", Rol.CLIENTE, localId = 2),
    )

    private val locales = listOf(
        Local(1, "Supermercado Central Maipú", "Av. Pajaritos 2450", "Maipú", "Jorge Díaz", "+56 9 8123 4567"),
        Local(2, "Mall Costanera Oeste", "Av. Américo Vespucio 1501", "Cerrillos", "Andrea Pino", "+56 9 7345 1122"),
        Local(3, "Tienda Retail Providencia", "Av. Providencia 1980", "Providencia", "Felipe Vera", "+56 9 6012 3344"),
    )

    private fun equipos() = listOf(
        Equipo(1, 1, TipoEquipo.REFRIGERACION, "Bitzer", "Cámara de frío CF-200", UbicacionEquipo.INTERIOR, fecha(-80, 10)),
        Equipo(2, 1, TipoEquipo.CLIMATIZACION, "Carrier", "Condensadora 38CKS", UbicacionEquipo.EXTERIOR, fecha(-120, 10)),
        Equipo(3, 2, TipoEquipo.CLIMATIZACION, "Daikin", "Chiller EWAD-TZ", UbicacionEquipo.EXTERIOR, fecha(-60, 10)),
        Equipo(4, 2, TipoEquipo.CLIMATIZACION, "Trane", "Manejadora UMA-3", UbicacionEquipo.INTERIOR, fecha(-30, 10)),
        Equipo(5, 3, TipoEquipo.REFRIGERACION, "Danfoss", "Vitrina refrigerada VR-4", UbicacionEquipo.INTERIOR, fecha(-45, 10)),
        Equipo(6, 3, TipoEquipo.CLIMATIZACION, "LG", "Split muro 24.000 BTU", UbicacionEquipo.INTERIOR, fecha(-200, 10)),
    )

    private fun ordenes() = listOf(
        OrdenTrabajo(
            id = 1, numero = "OT-2026-0101", localId = 1, equipoId = 2, tecnicoId = 1,
            tipo = TipoOT.CORRECTIVA, prioridad = Prioridad.ALTA, estado = EstadoOT.ASIGNADA,
            descripcion = "Condensadora no enciende tras corte de luz. Local reporta temperatura alta en sala de ventas.",
            fechaCreacion = fecha(-1, 8), fechaProgramada = fecha(0, 9), falla = "Eléctrica"
        ),
        OrdenTrabajo(
            id = 2, numero = "OT-2026-0102", localId = 2, equipoId = 3, tecnicoId = 1,
            tipo = TipoOT.PREVENTIVA, prioridad = Prioridad.MEDIA, estado = EstadoOT.ACEPTADA, fechaAceptacion = System.currentTimeMillis() - 3_600_000,
            descripcion = "Mantención preventiva trimestral del chiller: limpieza de serpentines y revisión de presiones.",
            fechaCreacion = fecha(-3, 9), fechaProgramada = fecha(0, 11, 30)
        ),
        OrdenTrabajo(
            id = 3, numero = "OT-2026-0103", localId = 3, equipoId = 5, tecnicoId = 1,
            tipo = TipoOT.CORRECTIVA, prioridad = Prioridad.ALTA, estado = EstadoOT.ACEPTADA, fechaAceptacion = System.currentTimeMillis() - 3_600_000,
            descripcion = "Vitrina refrigerada no mantiene temperatura (marca 9 °C, debe estar bajo 5 °C).",
            fechaCreacion = fecha(-1, 15), fechaProgramada = fecha(0, 15)
        ),
        OrdenTrabajo(
            id = 4, numero = "OT-2026-0098", localId = 1, equipoId = 1, tecnicoId = 1,
            tipo = TipoOT.PREVENTIVA, prioridad = Prioridad.BAJA, estado = EstadoOT.CERRADA, fechaAceptacion = System.currentTimeMillis() - 3_600_000,
            descripcion = "Revisión mensual cámara de frío.",
            fechaCreacion = fecha(-5, 9), fechaProgramada = fecha(-3, 10),
            checkInFechaHora = fecha(-3, 10, 6), checkOutFechaHora = fecha(-3, 10, 58),
            diagnostico = "Equipo operando normal, se limpió evaporador y se ajustó termostato.",
            actividadesRealizadas = "Limpieza de evaporador, ajuste de termostato.",
            materialesUtilizados = "Sin materiales"
        ),
        OrdenTrabajo(
            id = 5, numero = "OT-2026-0104", localId = 2, equipoId = 4, tecnicoId = null,
            tipo = TipoOT.CORRECTIVA, prioridad = Prioridad.MEDIA, estado = EstadoOT.CREADA,
            descripcion = "Ruido anormal en manejadora del segundo piso, reportado por administración.",
            fechaCreacion = fecha(0, 8), fechaProgramada = fecha(1, 10)
        ),
        OrdenTrabajo(
            id = 6, numero = "OT-2026-0099", localId = 3, equipoId = 6, tecnicoId = 3,
            tipo = TipoOT.PREVENTIVA, prioridad = Prioridad.BAJA, estado = EstadoOT.VISITA_FALLIDA, fechaAceptacion = System.currentTimeMillis() - 3_600_000,
            descripcion = "Mantención preventiva split oficina gerencia.",
            fechaCreacion = fecha(-3, 9), fechaProgramada = fecha(-1, 16), motivo = "Local cerrado"
        ),
        OrdenTrabajo(
            id = 7, numero = "OT-2026-0097", localId = 2, equipoId = 3, tecnicoId = 3,
            tipo = TipoOT.CORRECTIVA, prioridad = Prioridad.ALTA, estado = EstadoOT.POSTERGADA,
            descripcion = "Fuga de refrigerante en chiller de azotea.",
            fechaCreacion = fecha(-2, 9), fechaProgramada = fecha(2, 9), motivo = "Lluvia intensa en la zona"
        ),
    )

    // El orden importa: primero las tablas "padre" (usuarios, locales) y después las que las referencian
    suspend fun cargar(db: AppDatabase) {
        db.usuarioDao().insertAll(usuarios)
        db.localDao().insertAll(locales)
        db.equipoDao().insertAll(equipos())
        db.ordenTrabajoDao().insertAll(ordenes())
    }
}
