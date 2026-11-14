package cl.duoc.acis.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Looper
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Lo que el check-in necesita del GPS. Es una interfaz para poder probar el check-in
 * con una ubicación falsa, sin GPS real.
 */
interface FuenteUbicacion {
    fun estaActivada(): Boolean
    suspend fun obtener(): Location?
}

/**
 * Lee la ubicación del teléfono UNA sola vez (para el check-in). No sigue al técnico durante el día.
 * Usa LocationManager de Android: no necesita librerías extra ni Google Play Services,
 * por eso funciona en cualquier emulador. Recibe el applicationContext, no una pantalla.
 */
class ServicioUbicacion(context: Context) : FuenteUbicacion {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    /** ¿Está encendida la ubicación del teléfono (GPS o red)? */
    override fun estaActivada(): Boolean =
        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

    /**
     * Pide la ubicación actual. Devuelve null si no llega en 15 segundos (por ejemplo, sin señal).
     * La pantalla ya revisó el permiso antes de llamar a esta función.
     */
    @SuppressLint("MissingPermission")
    override suspend fun obtener(): Location? {
        val proveedor = if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            LocationManager.GPS_PROVIDER else LocationManager.NETWORK_PROVIDER

        return withTimeoutOrNull(15_000) {
            suspendCancellableCoroutine { cont ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // Android 11 o superior: pide una sola ubicación nueva
                    val cancelar = CancellationSignal()
                    cont.invokeOnCancellation { cancelar.cancel() }
                    locationManager.getCurrentLocation(proveedor, cancelar, { it.run() }) { ubicacion ->
                        if (cont.isActive) cont.resume(ubicacion)
                    }
                } else {
                    // Android 8 a 10: se escucha la primera ubicación y se deja de escuchar
                    val listener = object : LocationListener {
                        override fun onLocationChanged(ubicacion: Location) {
                            locationManager.removeUpdates(this)
                            if (cont.isActive) cont.resume(ubicacion)
                        }
                    }
                    cont.invokeOnCancellation { locationManager.removeUpdates(listener) }
                    locationManager.requestLocationUpdates(proveedor, 0L, 0f, listener, Looper.getMainLooper())
                }
            }
        }
    }
}

/** Coordenadas con 5 decimales (unos 1 m de precisión), ej.: "-33.51100, -70.75750". */
fun formatoCoordenadas(lat: Double, lon: Double): String =
    String.format(Locale.US, "%.5f, %.5f", lat, lon)
