# ACIS OT Terreno

App Android para gestionar órdenes de trabajo (OT) en terreno de ACIS Servicios Técnicos Ltda., empresa de mantenimiento de climatización y refrigeración comercial.

- **Autor:** Luis Roa Riquelme
- **Asignatura:** DSY1105 Desarrollo de Aplicaciones Móviles, Duoc UC
- **Evaluación:** Parcial 2 (encargo) y Parcial 3 (defensa)

Hecha con Kotlin, Jetpack Compose y Material 3. Los datos se guardan en el celular con Room; no hay servidor.

## Qué hace

**Técnico**
- Ve sus OT del día, con la próxima visita destacada y filtros (Pendientes, En ejecución, Cerradas).
- Acepta o rechaza una OT asignada (el rechazo pide motivo).
- Hace **check-in con GPS** al llegar al local: se guardan la hora y las coordenadas.
- Registra una visita fallida si no pudo atender (con motivo).
- Escribe el diagnóstico y las actividades, toma una **foto de evidencia** con la cámara y hace el **check-out**.
- Consulta el historial de OT de cada local.
- En su perfil ve sus datos y para qué usa la app cada permiso.

**Jefe de mantenimiento**
- Ve todas las OT con filtros (Todas, Sin asignar, En curso, Por revisar, Cerradas).
- Revisa la evidencia de cada OT cerrada y da **conformidad** o **disconformidad** (con motivo, la OT vuelve al técnico).
- Crea, edita, asigna, reasigna, posterga y cancela OT.
- Administra los equipos de cada local (crear, editar, eliminar o dar de baja) y ve su historial.

**Cliente (administrador de un local)**
- Solo consulta: ve el avance de las OT de su local (estado, técnico, check-in, diagnóstico y foto), sin poder modificarlas.

## Cómo cumple la rúbrica

| Requisito | Dónde está |
|---|---|
| Diseño visual coherente | Material 3, tema propio (`ui/theme`), componentes reutilizables (`view/components`) |
| Formularios validados | Login, crear/editar OT, diagnóstico y equipo: error por campo con ícono, botón activo solo si todo es válido (`util/Validaciones.kt`) |
| Navegación funcional | Navigation Compose con rutas en una `sealed class` y barra inferior según el rol (`navigation/`) |
| Gestión de estado | MVVM: un ViewModel por pantalla con `StateFlow` y `mutableStateOf` (`viewmodel/`) |
| Almacenamiento local | Room con 4 tablas, llaves foráneas y datos de ejemplo (`data/`, `model/`) |
| Recursos nativos (2) | **GPS** para el check-in y **cámara** para la foto de evidencia, con permisos en tiempo de ejecución |
| Animaciones | Transiciones entre pantallas, lista animada al filtrar, cambio de estado, línea de progreso y logo (`view/components/Animaciones.kt`) |

## Cómo correrla

1. Abrir la carpeta `ACIS_OT_Terreno` con Android Studio. Usa las mismas versiones que el ejemplo del curso: AGP 9.2.1, Gradle 9.4.1, Kotlin 2.2.10 y compileSdk 36.1.
2. Esperar a que termine la sincronización de Gradle. La primera vez descarga dependencias, así que necesita internet.
3. Elegir un emulador o un celular con Android 8.0 (API 26) o superior y presionar **Run**.

Por consola también se puede compilar y correr los tests:

```
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

El APK queda en `app/build/outputs/apk/debug/`.

## Usuarios de prueba

| Rol | Correo | Contraseña |
|---|---|---|
| Técnico | tecnico@acis.cl | 123456 |
| Jefe de mantenimiento | jefe@acis.cl | 123456 |
| Cliente (Mall Costanera Oeste) | cliente@acis.cl | 123456 |

Los usuarios se cargan solos en la base de datos la primera vez que se abre la app. Las contraseñas están en texto plano porque es una demo local, sin servidor.

## Probar los recursos nativos en el emulador

**GPS (check-in):** el emulador no tiene GPS físico, así que la ubicación se simula. Con el emulador abierto, en **⋯ (Extended controls) → Location**, buscar una dirección (por ejemplo "Av. Pajaritos 2450, Maipú") y presionar **Set location**. Luego, como técnico, abrir una OT aceptada y tocar **Hacer check-in**.

**Cámara (foto de evidencia):** el emulador trae una cámara virtual. Para usar la webcam del computador: Device Manager → editar el emulador → Show Advanced Settings → Camera → Back: **Webcam0**. Luego, con una OT en ejecución, tocar **Registrar diagnóstico y cerrar → Tomar foto**.

En ambos casos la app primero explica para qué usa el permiso, después lo pide, y si se rechaza ofrece ir a Ajustes.

## Reglas de negocio principales

- **Máquina de estados de la OT** (`model/MaquinaEstadosOT.kt`): una tabla define desde qué estado se puede pasar a cuál, qué rol lo hace y si pide motivo. El repositorio la revisa contra el estado guardado antes de cada cambio, y la pantalla muestra solo los botones permitidos.
- **Equipos:** un equipo con OT no se borra, se da de baja para conservar su historial.
- **Fechas:** una OT no se programa en el pasado; la última mantención de un equipo no puede ser futura.
- **Foto:** es obligatoria para cerrar una OT y se guarda en la carpeta privada de la app (no en la galería), mediante un `FileProvider`.

## Estructura

Organizada con el patrón MVVM, con los mismos nombres de carpetas que el ejemplo del curso:

```
app/src/main/java/cl/duoc/acis/
├── model/         tablas de Room (Usuario, Local, Equipo, OrdenTrabajo), sus opciones (Rol, EstadoOT…)
│                  y la máquina de estados de la OT (MaquinaEstadosOT)
├── data/          AppDatabase, DAOs (consultas) y datos de ejemplo
├── repository/    único punto de acceso a los datos para los ViewModel
├── viewmodel/     lógica y estado de cada pantalla
├── navigation/    rutas (sealed class) y navegación con barra inferior
├── view/          pantallas (una por archivo)
│   └── components/  piezas reutilizables (campos validados, tarjetas, foto, animaciones)
├── util/          validaciones, fechas, ubicación y fotos
├── ui/theme/      colores, tema y tipografía
└── MainActivity.kt
```

## Tests

43 tests unitarios en `app/src/test`, que corren sin emulador:

- `MaquinaEstadosOTTest` (21): rutas permitidas y prohibidas de la OT, conformidad, cliente sin acciones, estados finales, ningún estado huérfano y que el formulario de edición nunca invente una ruta.
- `ValidacionesTest` (14): correo, contraseña, fechas, descripción, diagnóstico, foto y datos del equipo.
- `UbicacionTest` (1): formato de las coordenadas.
- `InicialesTest` (1): iniciales del avatar del perfil.
- `PasoAlcanzadoTest` (3): hasta qué paso llegó una OT detenida.
- `DuracionTest` (3): duración de la atención ("menos de 1 min", "45 min", "1 h 20 min").
