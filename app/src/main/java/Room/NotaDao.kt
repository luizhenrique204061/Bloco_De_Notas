package Room

import Modelo.Notas
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface NotaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salva(notas: Notas)

    @Query("SELECT * FROM tabela_notas")
    suspend fun buscarTodas(): MutableList<Notas>

    @Query("DELETE FROM tabela_notas WHERE id = :id")
    suspend fun remover(id: Long)

}