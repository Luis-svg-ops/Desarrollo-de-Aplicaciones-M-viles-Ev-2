package cl.duoc.acis.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import cl.duoc.acis.model.Equipo
import cl.duoc.acis.model.Local
import cl.duoc.acis.model.OrdenTrabajo
import cl.duoc.acis.model.Usuario
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Usuario::class, Local::class, Equipo::class, OrdenTrabajo::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun localDao(): LocalDao
    abstract fun equipoDao(): EquipoDao
    abstract fun ordenTrabajoDao(): OrdenTrabajoDao

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
                    // La primera vez que se crea la base, se cargan los datos de ejemplo
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.let { DatosEjemplo.cargar(it) }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
