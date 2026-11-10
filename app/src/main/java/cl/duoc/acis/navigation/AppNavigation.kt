package cl.duoc.acis.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cl.duoc.acis.data.AppDatabase
import cl.duoc.acis.model.Rol
import cl.duoc.acis.repository.EquipoRepository
import cl.duoc.acis.repository.LocalRepository
import cl.duoc.acis.repository.OrdenTrabajoRepository
import cl.duoc.acis.repository.UsuarioRepository
import cl.duoc.acis.view.DetalleOtScreen
import cl.duoc.acis.view.DiagnosticoScreen
import cl.duoc.acis.view.EquipoFormScreen
import cl.duoc.acis.view.LocalDetalleScreen
import cl.duoc.acis.view.LocalesScreen
import cl.duoc.acis.view.LoginScreen
import cl.duoc.acis.view.OtFormScreen
import cl.duoc.acis.view.ListaOtScreen
import cl.duoc.acis.view.PerfilScreen
import cl.duoc.acis.viewmodel.DetalleOtViewModel
import cl.duoc.acis.viewmodel.ListaOtViewModel
import cl.duoc.acis.viewmodel.LoginViewModel
import cl.duoc.acis.viewmodel.OtFormViewModel

/**
 * Fábrica de ViewModels (el mismo Factory del ejemplo del curso, escrito una sola vez).
 * Android crea los ViewModel por su cuenta; esta "receta" le dice cómo construir cada uno
 * con sus repositorios.
 */
private fun fabrica(crear: () -> ViewModel) = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = crear() as T
}

private data class ItemBarra(val ruta: String, val texto: String, val icono: ImageVector)

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    // Base de datos y repositorios: se crean una sola vez (igual que en el ejemplo del curso)
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val usuarioRepository = remember { UsuarioRepository(database.usuarioDao()) }
    val ordenTrabajoRepository = remember { OrdenTrabajoRepository(database.ordenTrabajoDao()) }
    val localRepository = remember { LocalRepository(database.localDao()) }
    val equipoRepository = remember { EquipoRepository(database.equipoDao(), database.ordenTrabajoDao()) }

    // Sesión: quién inició sesión y con qué rol. Sobrevive a la rotación de pantalla.
    var rol by rememberSaveable { mutableStateOf<Rol?>(null) }
    var usuarioId by rememberSaveable { mutableLongStateOf(0L) }

    val entrada by navController.currentBackStackEntryAsState()
    val rutaActual = entrada?.destination?.route
    val esJefe = rol == Rol.JEFE

    val items = if (esJefe) listOf(
        ItemBarra(Rutas.Ots.route, "Órdenes", Icons.AutoMirrored.Filled.List),
        ItemBarra(Rutas.Locales.route, "Locales", Icons.Filled.Place),
        ItemBarra(Rutas.Perfil.route, "Perfil", Icons.Filled.Person),
    ) else listOf(
        ItemBarra(Rutas.Ots.route, "Mis OT", Icons.AutoMirrored.Filled.List),
        ItemBarra(Rutas.Locales.route, "Historial", Icons.Filled.DateRange),
        ItemBarra(Rutas.Perfil.route, "Perfil", Icons.Filled.Person),
    )

    Scaffold(
        // Cada pantalla maneja su propia barra superior; aquí solo va la barra inferior
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (rutaActual in Rutas.principales) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    // Mismos colores que el mockup: la pestaña activa es una cápsula en el azul principal
                    // con ícono y texto en blanco. El indicador estándar solo cubre el ícono, así que
                    // el ícono y el texto van juntos dentro de la cápsula y el indicador se deja transparente.
                    val coloresItem = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    items.forEach { item ->
                        val seleccionado = rutaActual == item.ruta
                        NavigationBarItem(
                            selected = seleccionado,
                            onClick = {
                                navController.navigate(item.ruta) {
                                    // El inicio del grafo es LOGIN, que ya no está en la pila: se vuelve a OTS, la raíz tras el login
                                    popUpTo(Rutas.Ots.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Column(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(if (seleccionado) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .padding(horizontal = 20.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(item.icono, contentDescription = null)
                                    Text(item.texto, style = MaterialTheme.typography.labelMedium)
                                }
                            },
                            label = null,
                            colors = coloresItem
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Rutas.Login.route,
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)
        ) {
            composable(Rutas.Login.route) {
                // Un ViewModel propio para esta pantalla. La fábrica le entrega el repositorio
                val loginViewModel: LoginViewModel = viewModel(factory = fabrica { LoginViewModel(usuarioRepository) })
                LoginScreen(viewModel = loginViewModel, onLoginExitoso = { usuario ->
                    rol = usuario.rol
                    usuarioId = usuario.id
                    navController.navigate(Rutas.Ots.route) { popUpTo(Rutas.Login.route) { inclusive = true } }
                })
            }
            composable(Rutas.Ots.route) {
                val listaViewModel: ListaOtViewModel = viewModel(
                    factory = fabrica { ListaOtViewModel(ordenTrabajoRepository, usuarioId, rol ?: Rol.TECNICO) }
                )
                ListaOtScreen(
                    viewModel = listaViewModel,
                    esJefe = esJefe,
                    onVerOt = { navController.navigate(Rutas.DetalleOt.createRoute(it)) },
                    onNuevaOt = { navController.navigate(Rutas.OtForm.createRoute()) }
                )
            }
            composable(Rutas.Locales.route) {
                LocalesScreen(esJefe = esJefe, onVerLocal = { navController.navigate(Rutas.Local.createRoute(it)) })
            }
            composable(Rutas.Perfil.route) {
                PerfilScreen(rol = rol, onCerrarSesion = {
                    rol = null
                    usuarioId = 0L
                    navController.navigate(Rutas.Login.route) { popUpTo(0) { inclusive = true } }
                })
            }
            composable(
                Rutas.DetalleOt.route,
                arguments = listOf(navArgument("otId") { type = NavType.LongType })
            ) { backStack ->
                val otId = backStack.arguments?.getLong("otId") ?: 0L
                val detalleViewModel: DetalleOtViewModel = viewModel(
                    factory = fabrica { DetalleOtViewModel(ordenTrabajoRepository, otId, rol ?: Rol.TECNICO, usuarioId) }
                )
                DetalleOtScreen(
                    viewModel = detalleViewModel,
                    onVolver = { navController.popBackStack() },
                    onEditar = { navController.navigate(Rutas.OtForm.createRoute(otId)) },
                    onDiagnostico = { navController.navigate(Rutas.Diagnostico.createRoute(otId)) }
                )
            }
            composable(
                Rutas.OtForm.route,
                arguments = listOf(navArgument("otId") { type = NavType.LongType; defaultValue = -1L })
            ) { backStack ->
                val otId = backStack.arguments?.getLong("otId")?.takeIf { it > 0 }
                val formViewModel: OtFormViewModel = viewModel(
                    factory = fabrica {
                        OtFormViewModel(ordenTrabajoRepository, localRepository, equipoRepository, usuarioRepository, otId)
                    }
                )
                OtFormScreen(viewModel = formViewModel, onVolver = { navController.popBackStack() })
            }
            composable(
                Rutas.Diagnostico.route,
                arguments = listOf(navArgument("otId") { type = NavType.LongType })
            ) { backStack ->
                DiagnosticoScreen(
                    otId = backStack.arguments?.getLong("otId") ?: 0L,
                    onVolver = { navController.popBackStack() },
                    // Tras el check-out se vuelve a la lista sacando Diagnóstico y Detalle de la pila
                    onCheckOut = { navController.popBackStack(Rutas.Ots.route, inclusive = false) }
                )
            }
            composable(
                Rutas.Local.route,
                arguments = listOf(navArgument("localId") { type = NavType.LongType })
            ) { backStack ->
                val localId = backStack.arguments?.getLong("localId") ?: 0L
                LocalDetalleScreen(
                    localId = localId,
                    esJefe = esJefe,
                    onVolver = { navController.popBackStack() },
                    onVerOt = { navController.navigate(Rutas.DetalleOt.createRoute(it)) },
                    onEquipo = { equipoId -> navController.navigate(Rutas.EquipoForm.createRoute(localId, equipoId)) }
                )
            }
            composable(
                Rutas.EquipoForm.route,
                arguments = listOf(
                    navArgument("localId") { type = NavType.LongType },
                    navArgument("equipoId") { type = NavType.LongType; defaultValue = -1L }
                )
            ) { backStack ->
                EquipoFormScreen(
                    localId = backStack.arguments?.getLong("localId") ?: 0L,
                    equipoId = backStack.arguments?.getLong("equipoId")?.takeIf { it > 0 },
                    onVolver = { navController.popBackStack() }
                )
            }
        }
    }
}
