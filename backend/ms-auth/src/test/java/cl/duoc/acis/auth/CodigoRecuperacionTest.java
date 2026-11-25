package cl.duoc.acis.auth;

import cl.duoc.acis.auth.model.CodigoRecuperacion;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas unitarias puras (sin Spring ni base de datos) de las reglas del código. */
class CodigoRecuperacionTest {

    private final Instant ahora = Instant.parse("2026-11-25T12:00:00Z");

    private CodigoRecuperacion nuevo() {
        return new CodigoRecuperacion(null, "123456", ahora.plus(Duration.ofMinutes(10)));
    }

    @Test
    void sirveAntesDeVencer_yNoDespues() {
        CodigoRecuperacion c = nuevo();
        assertTrue(c.sirve(ahora.plus(Duration.ofMinutes(9))));
        assertFalse(c.sirve(ahora.plus(Duration.ofMinutes(10))));
    }

    @Test
    void cuatroFallosTodaviaSirve_elQuintoLoInvalida() {
        CodigoRecuperacion c = nuevo();
        for (int i = 0; i < 4; i++) c.registrarIntentoFallido();
        assertTrue(c.sirve(ahora));
        c.registrarIntentoFallido();
        assertFalse(c.sirve(ahora));
    }

    @Test
    void usadoUnaVez_yaNoSirve() {
        CodigoRecuperacion c = nuevo();
        c.marcarUsado();
        assertFalse(c.sirve(ahora));
    }

    @Test
    void comparaElCodigoExacto() {
        assertTrue(nuevo().coincide("123456"));
        assertFalse(nuevo().coincide("654321"));
    }
}
