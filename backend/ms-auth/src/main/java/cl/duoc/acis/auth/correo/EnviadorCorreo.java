package cl.duoc.acis.auth.correo;

/**
 * Polimorfismo: una sola "forma" (esta interfaz) con dos comportamientos.
 * - CorreoConsola: lo escribe en la consola del backend (simulado, siempre funciona).
 * - CorreoGmail: lo envía de verdad por Gmail.
 * Los servicios solo llaman a enviar() y no saben cuál de los dos se está usando;
 * se elige con la propiedad acis.correo.modo, sin tocar código.
 */
public interface EnviadorCorreo {

    void enviar(String destinatario, String asunto, String texto);
}
