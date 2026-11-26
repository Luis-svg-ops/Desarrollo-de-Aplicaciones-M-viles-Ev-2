package cl.duoc.acis.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import cl.duoc.acis.model.Equipo
import cl.duoc.acis.model.EventoOT
import cl.duoc.acis.model.Local
import cl.duoc.acis.model.OrdenTrabajo
import cl.duoc.acis.model.Usuario

@Database(
    entities = [Usuario::class, Local::class, Equipo::class, OrdenTrabajo::class, EventoOT::class],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun localDao(): LocalDao
    abstract fun equipoDao(): EquipoDao
    abstract fun ordenTrabajoDao(): OrdenTrabajoDao
    abstract fun eventoDao(): EventoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Una sola instancia de la base para toda la app (igual que el ejemplo del curso)
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "acis_database"
                )
                    // Mientras se desarrolla: si cambia una tabla, la base se recrea en vez de cerrarse la app
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    // Ya no se cargan datos de ejemplo: desde la EFT la base local es una COPIA
                    // de lo que entrega el servidor (se llena al iniciar sesión y al refrescar).
                    // DatosEjemplo se sigue usando en las pruebas.
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
