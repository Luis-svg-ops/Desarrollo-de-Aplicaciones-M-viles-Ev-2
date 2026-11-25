package cl.duoc.acis.auth.service;

import java.util.Optional;

/**
 * Busca a qué local corresponde un código (los locales viven en ms-ordenes).
 * Es una interfaz para que en las pruebas se pueda reemplazar por una falsa (mock),
 * sin necesitar ms-ordenes encendido.
 */
public interface DirectorioLocales {

    /** Id del local con ese código, o vacío si no existe. */
    Optional<Long> buscarPorCodigo(String codigo);
}
