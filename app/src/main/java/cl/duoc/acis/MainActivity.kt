package cl.duoc.acis

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cl.duoc.acis.navigation.AppNavigation
import cl.duoc.acis.ui.theme.AcisTheme

// Cuando se implemente la huella (BiometricPrompt) debe heredar de FragmentActivity
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // La barra superior siempre es oscura (azul principal en tema claro, gris azulado en tema oscuro),
        // así que los íconos de la barra de estado van claros
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        setContent {
            AcisTheme {
                AppNavigation()
            }
        }
    }
}
