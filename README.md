# ACIS OT Terreno

App Android para gestionar órdenes de trabajo (OT) en terreno de ACIS Servicios Técnicos Ltda., empresa de mantenimiento de climatización y refrigeración comercial. Proyecto de la asignatura DSY1105 Desarrollo de Aplicaciones Móviles (Duoc UC), Evaluación Parcial 2.

- **Técnico:** ve sus OT del día, hace check-in con GPS, registra el diagnóstico con foto de evidencia y hace el check-out.
- **Jefe de mantenimiento:** crea, asigna y supervisa las OT, y administra locales y equipos.

Hecha con Kotlin, Jetpack Compose y Material 3. Los datos se guardan en el celular con Room; no hay servidor.

> **Estado:** en desarrollo. Hoy están la estructura, el tema, la navegación, las pantallas de base (hito 1) la base de datos local con Room y datos de ejemplo (hito 2) el login con ViewModel y validaciones (hito 3) y la lista y el detalle de OT con datos reales (hito 4): filtros por estado, tarjeta de próxima visita, línea de progreso y aceptar una OT. Además, el formulario para crear y editar OT (hito 5) con validación por campo, equipo filtrado por local, calendario y reloj de Material 3. Los cambios de estado (aceptar, rechazar, visita fallida, postergar, cancelar y reasignar) los controla una máquina de estados con tests. El check-in con GPS (hito 6) pide el permiso de ubicación en tiempo de ejecución y guarda hora y coordenadas. El cierre con foto y el resto de las funciones llegan en los hitos siguientes.

## Cómo correrla

1. Abrir la carpeta `ACIS_OT_Terreno` con Android Studio. Usa las mismas versiones que el ejemplo del curso: AGP 9.2.1, Gradle 9.4.1, Kotlin 2.2.10 y compileSdk 36.1.
2. Esperar a que termine la sincronización de Gradle. La primera vez descarga dependencias, así que necesita internet.
3. Elegir un emulador o un celular con Android 8.0 (API 26) o superior y presionar **Run**.

Por consola también se puede compilar con:

```
./gradlew assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/`.

## Probar el check-in en el emulador

El emulador no tiene GPS físico: la ubicación se simula. Con el emulador abierto, en **⋯ (Extended controls) → Location**, buscar una dirección (por ejemplo "Av. Pajaritos 2450, Maipú") y presionar **Set location**. Luego, como técnico, abrir una OT aceptada y tocar **Hacer check-in**.

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
├── model/         tablas de Room (Usuario, Local, Equipo, OrdenTrabajo), sus opciones (Rol, EstadoOT…)
│                  y la máquina de estados de la OT (MaquinaEstadosOT)
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
- `model/MaquinaEstadosOT.kt`: tabla de qué estado puede pasar a cuál, quién lo hace y si pide motivo. El repositorio la revisa antes de cada cambio y la pantalla muestra solo los botones permitidos. Tests en `app/src/test/.../model`.
