package cl.duoc.acis.ordenes.service;

import java.util.Optional;

/**
 * Consulta a ms-auth si un técnico existe y está activo, y cuál es su nombre.
 * Es una interfaz para poder reemplazarla por una falsa (mock) en las pruebas.
 */
public interface DirectorioTecnicos {

    /** Nombre del técnico activo con ese id, o vacío si no existe o está desactivado. */
    Optional<String> nombreDe(Long tecnicoId, String tokenDelJefe);
}
