package cl.duoc.acis.ordenes.config;

import cl.duoc.acis.ordenes.model.Equipo;
import cl.duoc.acis.ordenes.model.EstadoOT;
import cl.duoc.acis.ordenes.model.EventoOT;
import cl.duoc.acis.ordenes.model.Local;
import cl.duoc.acis.ordenes.model.OrdenTrabajo;
import cl.duoc.acis.ordenes.model.Prioridad;
import cl.duoc.acis.ordenes.model.TipoEquipo;
import cl.duoc.acis.ordenes.model.TipoOT;
import cl.duoc.acis.ordenes.model.UbicacionEquipo;
import cl.duoc.acis.ordenes.repository.EquipoRepository;
import cl.duoc.acis.ordenes.repository.EventoRepository;
import cl.duoc.acis.ordenes.repository.LocalRepository;
import cl.duoc.acis.ordenes.repository.OrdenTrabajoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Datos de ejemplo (los mismos de la app). Se cargan solo si la base está vacía.
 * Técnicos (ids de ms-auth): 1 Pedro Soto, 3 Matías Rojas.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final LocalRepository locales;
    private final EquipoRepository equipos;
    private final OrdenTrabajoRepository ordenes;
    private final EventoRepository eventos;

    public DataInitializer(LocalRepository locales, EquipoRepository equipos, OrdenTrabajoRepository ordenes,
                           EventoRepository eventos) {
        this.locales = locales;
        this.equipos = equipos;
        this.ordenes = ordenes;
        this.eventos = eventos;
    }

    /** Fecha relativa a hoy, en milisegundos (como en la app). */
    private static long fecha(long dias, int hora, int minuto) {
        return LocalDate.now().plusDays(dias).atTime(hora, minuto).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private static long fecha(long dias, int hora) {
        return fecha(dias, hora, 0);
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (locales.count() > 0) {
            return;
        }
        Local maipu = locales.save(new Local("MAIPU-01", "Supermercado Central Maipú", "Av. Pajaritos 2450", "Maipú",
                "Jorge Díaz", "+56 9 8123 4567", -33.5110, -70.7575));
        Local mall = locales.save(new Local("MALL-02", "Mall Costanera Oeste", "Av. Américo Vespucio 1501", "Cerrillos",
                "Andrea Pino", "+56 9 7345 1122", -33.4970, -70.7170));
        Local prov = locales.save(new Local("PROV-03", "Tienda Retail Providencia", "Av. Providencia 1980", "Providencia",
                "Felipe Vera", "+56 9 6012 3344", -33.4233, -70.6110));

        Equipo camara = equipos.save(new Equipo(maipu, TipoEquipo.REFRIGERACION, "Bitzer", "Cámara de frío CF-200", UbicacionEquipo.INTERIOR, fecha(-80, 10)));
        Equipo condensadora = equipos.save(new Equipo(maipu, TipoEquipo.CLIMATIZACION, "Carrier", "Condensadora 38CKS", UbicacionEquipo.EXTERIOR, fecha(-120, 10)));
        Equipo chiller = equipos.save(new Equipo(mall, TipoEquipo.CLIMATIZACION, "Daikin", "Chiller EWAD-TZ", UbicacionEquipo.EXTERIOR, fecha(-60, 10)));
        Equipo manejadora = equipos.save(new Equipo(mall, TipoEquipo.CLIMATIZACION, "Trane", "Manejadora UMA-3", UbicacionEquipo.INTERIOR, fecha(-30, 10)));
        Equipo vitrina = equipos.save(new Equipo(prov, TipoEquipo.REFRIGERACION, "Danfoss", "Vitrina refrigerada VR-4", UbicacionEquipo.INTERIOR, fecha(-45, 10)));
        Equipo split = equipos.save(new Equipo(prov, TipoEquipo.CLIMATIZACION, "LG", "Split muro 24.000 BTU", UbicacionEquipo.INTERIOR, fecha(-200, 10)));

        long haceUnaHora = System.currentTimeMillis() - 3_600_000;

        ot(condensadora, 1L, "Pedro Soto", TipoOT.CORRECTIVA, Prioridad.ALTA, EstadoOT.ASIGNADA, "OT-2026-0101",
                "Condensadora no enciende tras corte de luz. Local reporta temperatura alta en sala de ventas.",
                "Eléctrica", fecha(-1, 8), fecha(0, 9), null, null, null, null, "Asignada a Pedro Soto");
        ot(chiller, 1L, "Pedro Soto", TipoOT.PREVENTIVA, Prioridad.MEDIA, EstadoOT.ACEPTADA, "OT-2026-0102",
                "Mantención preventiva trimestral del chiller: limpieza de serpentines y revisión de presiones.",
                null, fecha(-3, 9), fecha(0, 11, 30), haceUnaHora, null, null, null, "Aceptada por Pedro Soto");
        ot(vitrina, 1L, "Pedro Soto", TipoOT.CORRECTIVA, Prioridad.ALTA, EstadoOT.ACEPTADA, "OT-2026-0103",
                "Vitrina refrigerada no mantiene temperatura (marca 9 °C, debe estar bajo 5 °C).",
                null, fecha(-1, 15), fecha(0, 15), haceUnaHora, null, null, null, "Aceptada por Pedro Soto");
        OrdenTrabajo cerrada = ot(camara, 1L, "Pedro Soto", TipoOT.PREVENTIVA, Prioridad.BAJA, EstadoOT.CERRADA, "OT-2026-0098",
                "Revisión mensual cámara de frío.", null, fecha(-5, 9), fecha(-3, 10), haceUnaHora,
                fecha(-3, 10, 6), fecha(-3, 10, 58), null, "Trabajo terminado por Pedro Soto (diagnóstico y foto)");
        cerrada.cargarEjemplo(EstadoOT.CERRADA, "OT-2026-0098", haceUnaHora, fecha(-3, 10, 6), fecha(-3, 10, 58),
                "Equipo operando normal, se limpió evaporador y se ajustó termostato.",
                "Limpieza de evaporador, ajuste de termostato.", "Sin materiales", null);
        ot(manejadora, null, null, TipoOT.CORRECTIVA, Prioridad.MEDIA, EstadoOT.CREADA, "OT-2026-0104",
                "Ruido anormal en manejadora del segundo piso, reportado por administración.",
                null, fecha(0, 8), fecha(1, 10), null, null, null, null, null);
        ot(split, 3L, "Matías Rojas", TipoOT.PREVENTIVA, Prioridad.BAJA, EstadoOT.VISITA_FALLIDA, "OT-2026-0099",
                "Mantención preventiva split oficina gerencia.", null, fecha(-3, 9), fecha(-1, 16), haceUnaHora,
                null, null, "Local cerrado", "Visita fallida: Local cerrado");
        ot(chiller, 3L, "Matías Rojas", TipoOT.CORRECTIVA, Prioridad.ALTA, EstadoOT.POSTERGADA, "OT-2026-0097",
                "Fuga de refrigerante en chiller de azotea.", null, fecha(-2, 9), fecha(2, 9), null,
                null, null, "Lluvia intensa en la zona", "Postergada: Lluvia intensa en la zona");
    }

    private OrdenTrabajo ot(Equipo equipo, Long tecnicoId, String tecnico, TipoOT tipo, Prioridad prioridad,
                            EstadoOT estado, String numero, String descripcion, String falla, long creada,
                            long programada, Long aceptada, Long checkIn, Long checkOut, String motivo,
                            String ultimoEvento) {
        OrdenTrabajo ot = new OrdenTrabajo(equipo.getLocal(), equipo, tecnicoId, tecnico, tipo, prioridad,
                descripcion, falla, creada, programada);
        ot.cargarEjemplo(estado, numero, aceptada, checkIn, checkOut, null, null, null, motivo);
        ordenes.save(ot);
        eventos.save(new EventoOT(ot, EstadoOT.CREADA, "Creada por la jefatura", creada));
        if (ultimoEvento != null) {
            long cuando = checkOut != null ? checkOut : checkIn != null ? checkIn : aceptada != null ? aceptada : creada;
            eventos.save(new EventoOT(ot, estado, ultimoEvento, cuando));
        }
        return ot;
    }
}
