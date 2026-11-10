package cl.duoc.acis.repository

import cl.duoc.acis.data.UsuarioDao
import cl.duoc.acis.model.Usuario
import kotlinx.coroutines.flow.Flow

/**
 * Único punto de acceso a los datos de usuarios. El ViewModel no habla directo con el DAO:
 * si mañana los datos vienen de un servidor, solo cambia esta clase.
 */
class UsuarioRepository(private val usuarioDao: UsuarioDao) {

    fun getTecnicos(): Flow<List<Usuario>> = usuarioDao.getTecnicos()

    suspend fun getById(id: Long): Usuario? = usuarioDao.getById(id)

    /** Devuelve el usuario si el correo y la contraseña coinciden; si no, null. */
    suspend fun login(email: String, password: String): Usuario? {
        val usuario = usuarioDao.getByEmail(email.trim().lowercase())
        return if (usuario != null && usuario.password == password) usuario else null
    }
}
