package cl.duoc.acis

import cl.duoc.acis.data.remote.EquipoDto
import cl.duoc.acis.data.remote.LocalDto
import cl.duoc.acis.data.remote.OtDto
import cl.duoc.acis.data.remote.UsuarioDto
import cl.duoc.acis.model.Equipo
import cl.duoc.acis.model.EstadoOT
import cl.duoc.acis.model.Local
import cl.duoc.acis.model.OrdenTrabajo
import cl.duoc.acis.model.OtConDetalle
import cl.duoc.acis.model.Prioridad
import cl.duoc.acis.model.Rol
import cl.duoc.acis.model.TipoEquipo
import cl.duoc.acis.model.TipoOT
import cl.duoc.acis.model.UbicacionEquipo
import cl.duoc.acis.model.Usuario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import retrofit2.HttpException
import retrofit2.Response

/**
 * Regla para probar ViewModels: viewModelScope usa Dispatchers.Main, que en una prueba
 * (sin Android) no existe. Se reemplaza por un dispatcher de prueba que ejecuta al tiro.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

/** Datos de muestra para las pruebas (los mismos nombres del ejemplo de la app). */
object Muestras {
    val local = Local(2, "Mall Costanera Oeste", "Av. Américo Vespucio 1501", "Cerrillos", "Andrea Pino", "+56 9 7345 1122",
        codigo = "MALL-02", latitud = -33.497, longitud = -70.717)
    val equipo = Equipo(3, 2, TipoEquipo.CLIMATIZACION, "Daikin", "Chiller EWAD-TZ", UbicacionEquipo.EXTERIOR)
    val pedro = Usuario(1, "Pedro Soto", "tecnico@acis.cl", "", Rol.TECNICO)

    fun ot(estado: EstadoOT = EstadoOT.ASIGNADA, tecnicoId: Long? = 1, id: Long = 2) = OrdenTrabajo(
        id = id, numero = "OT-2026-0102", localId = 2, equipoId = 3, tecnicoId = tecnicoId,
        tipo = TipoOT.PREVENTIVA, prioridad = Prioridad.MEDIA, estado = estado,
        descripcion = "Mantención preventiva del chiller", fechaCreacion = 0, fechaProgramada = System.currentTimeMillis()
    )

    fun detalle(estado: EstadoOT = EstadoOT.ASIGNADA) = OtConDetalle(ot(estado), local, equipo, pedro)

    private val localDto = LocalDto(2, "MALL-02", "Mall Costanera Oeste", "Av. Américo Vespucio 1501", "Cerrillos",
        "Andrea Pino", "+56 9 7345 1122", -33.497, -70.717)
    private val equipoDto = EquipoDto(3, 2, TipoEquipo.CLIMATIZACION, "Daikin", "Chiller EWAD-TZ", UbicacionEquipo.EXTERIOR, null, false)

    fun otDto(estado: EstadoOT = EstadoOT.ASIGNADA, id: Long = 2) = OtDto(
        id = id, numero = "OT-2026-0102", estado = estado, tipo = TipoOT.PREVENTIVA, prioridad = Prioridad.MEDIA,
        descripcion = "Mantención preventiva del chiller", falla = null, fechaCreacion = 0, fechaProgramada = 0,
        fechaAceptacion = null, checkInFechaHora = null, checkInLat = null, checkInLon = null, checkOutFechaHora = null,
        diagnostico = null, actividadesRealizadas = null, materialesUtilizados = null, fotoUri = null, motivo = null,
        tecnicoId = 1, tecnicoNombre = "Pedro Soto", local = localDto, equipo = equipoDto
    )

    fun usuarioDto(id: Long = 9, estado: String = "PENDIENTE", rol: Rol = Rol.CLIENTE) =
        UsuarioDto(id, "Sofia Lagos", "sofia@correo.cl", "+56 9 1234 5678", rol, estado, 2)

    /** Error HTTP como el que lanza Retrofit cuando el servidor responde 4xx con {"error": "..."}. */
    fun errorHttp(codigo: Int, mensaje: String? = null): HttpException {
        val cuerpo = ResponseBody.create(null, if (mensaje == null) "" else """{"status":$codigo,"error":"$mensaje"}""")
        return HttpException(Response.error<Any>(codigo, cuerpo))
    }
}
