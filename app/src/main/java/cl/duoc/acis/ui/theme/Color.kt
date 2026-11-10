package cl.duoc.acis.ui.theme

import androidx.compose.ui.graphics.Color

// Colores de marca (mismos que el mockup): azul principal saturado, fondos neutros y el naranjo como acento.
// El azul hielo se usa solo en la insignia de Refrigeración (más abajo), nunca en fondos grandes.
val AzulPrincipal = Color(0xFF1B6C9E) // barra superior, pestañas activas, botones e indicador de la barra inferior (blanco encima: 5.7:1)
val AzulProfundo = Color(0xFF0B2540)  // texto sobre contenedores claros
val NaranjoAcis = Color(0xFFE8791E)

// Tema claro
val PrimaryLight = AzulPrincipal
val OnPrimaryLight = Color.White
val PrimaryContainerLight = Color(0xFFDCE7F0) // gris azulado neutro para chips y fondos pequeños
val OnPrimaryContainerLight = AzulProfundo
val SecondaryLight = NaranjoAcis
val OnSecondaryLight = Color.White
val BackgroundLight = Color(0xFFF4F6F9)
val SurfaceLight = Color.White
val SurfaceVariantLight = Color(0xFFEEF1F5)
val OnSurfaceLight = Color(0xFF1A1C1E)
val OnSurfaceVariantLight = Color(0xFF5E6670)
val OutlineLight = Color(0xFFC3C8CF)
val ErrorLight = Color(0xFFBA1A1A)
val ErrorContainerLight = Color(0xFFFFDAD6)

// Tema oscuro
val PrimaryDark = Color(0xFF9CC4F0)
val OnPrimaryDark = Color(0xFF0B2540)
val PrimaryContainerDark = Color(0xFF244A70)
val OnPrimaryContainerDark = Color(0xFFD3E3F5)
val SecondaryDark = Color(0xFFFFA65C)
val OnSecondaryDark = Color(0xFF3A1A00)
val BackgroundDark = Color(0xFF111418)
val SurfaceDark = Color(0xFF1A1E23)
val SurfaceVariantDark = Color(0xFF232830)
val OnSurfaceDark = Color(0xFFE2E5E9)
val OnSurfaceVariantDark = Color(0xFFA3ABB5)
val OutlineDark = Color(0xFF424852)
val ErrorDark = Color(0xFFFFB4AB)
val ErrorContainerDark = Color(0xFF5C1712)

// Insignias de tipo de equipo (ver NOTAS_ANDROID.md)
val InsigniaRefrigeracion = Color(0xFF29A3D6)
val InsigniaClimatizacion = Color(0xFFEF6C00)
val InsigniaFallaElectrica = Color(0xFFE0A800)
val InsigniaExterior = Color(0xFF43A047)

// Franja de prioridad
val PrioridadAlta = Color(0xFFD32F2F)
val PrioridadMedia = Color(0xFFF2B705)
val PrioridadBaja = Color(0xFF2E9D4C)
