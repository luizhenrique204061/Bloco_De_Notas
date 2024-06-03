package Room

import Modelo.Notas
import Modelo.Tarefa
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface TarefaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvarTarefa(tarefa: Tarefa)

    @Query("SELECT * FROM tabela_tarefas")
    suspend fun buscarTodas(): MutableList<Tarefa>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirTodas(tarefa: MutableList<Tarefa>)

    @Query("DELETE FROM tabela_tarefas WHERE id = :id")
    suspend fun remover(id: Long)
}