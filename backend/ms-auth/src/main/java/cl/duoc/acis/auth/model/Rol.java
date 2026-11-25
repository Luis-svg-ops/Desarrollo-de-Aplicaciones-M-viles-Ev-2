package cl.duoc.acis.auth.model;

/** Los 4 roles de ACIS. Cada uno tiene permisos distintos (los revisa SecurityConfig y ms-ordenes). */
public enum Rol {
    /** Gestiona usuarios: crea técnicos y jefes, aprueba clientes, desactiva cuentas. */
    ADMIN,
    /** Jefe de mantenimiento: crea y asigna OT, da conformidad. */
    JEFE,
    /** Técnico de terreno: atiende las OT que tiene asignadas. */
    TECNICO,
    /** Cliente: ve el avance de las OT de su local. */
    CLIENTE
}
