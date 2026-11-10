# ACIS OT Terreno

App Android para gestionar órdenes de trabajo (OT) en terreno de ACIS Servicios Técnicos Ltda., empresa de mantenimiento de climatización y refrigeración comercial. Proyecto de la asignatura DSY1105 Desarrollo de Aplicaciones Móviles (Duoc UC), Evaluación Parcial 2.

- **Técnico:** ve sus OT del día, hace check-in con GPS, registra el diagnóstico con foto de evidencia y hace el check-out.
- **Jefe de mantenimiento:** crea, asigna y supervisa las OT, y administra locales y equipos.

Hecha con Kotlin, Jetpack Compose y Material 3. Los datos se guardan en el celular con Room; no hay servidor.

> **Estado:** en desarrollo. Hoy están la estructura, el tema, la navegación, las pantallas de base (hito 1) la base de datos local con Room y datos de ejemplo (hito 2) el login con ViewModel y validaciones (hito 3) y la lista y el detalle de OT con datos reales (hito 4): filtros por estado, tarjeta de próxima visita, línea de progreso y aceptar una OT. El formulario de login y el resto de las funciones llegan en los hitos siguientes.

## Cómo correrla

1. Abrir la carpeta `ACIS_OT_Terreno` con Android Studio. Usa las mismas versiones que el ejemplo del curso: AGP 9.2.1, Gradle 9.4.1, Kotlin 2.2.10 y compileSdk 36.1.
2. Esperar a que termine la sincronización de Gradle. La primera vez descarga dependencias, así que necesita internet.
3. Elegir un emulador o un celular con Android 8.0 (API 26) o superior y presionar **Run**.

Por consola también se puede compilar con:

```
./gradlew assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/`.

## Usuarios de prueba

| Rol | Correo | Contraseña |
|---|---|---|
| Técnico | tecnico@acis.cl | 123456 |
| Jefe de mantenimiento | jefe@acis.cl | 123456 |

Los usuarios se cargan solos en la base de datos la primera vez que se abre la app. Las contraseñas están en texto plano porque es una demo local, sin servidor.

## Estructura

Organizada con el patrón MVVM, con los mismos nombres de carpetas que el ejemplo del curso:

```
app/src/main/java/cl/duoc/acis/
├── model/         tablas de Room (Usuario, Local, Equipo, OrdenTrabajo) y sus opciones (Rol, EstadoOT…)
├── data/          AppDatabase, DAOs (consultas) y datos de ejemplo
├── repository/    único punto de acceso a los datos para los ViewModel
├── navigation/    rutas (sealed class) y navegación con barra inferior
├── view/          pantallas (una por archivo)
│   └── components/  piezas reutilizables (barra superior, logo, botones)
├── ui/theme/      colores, tema y tipografía
└── MainActivity.kt
```

- `viewmodel/`: lógica y estado de cada pantalla (StateFlow).
- `util/`: validaciones reutilizables, con tests en `app/src/test`.
