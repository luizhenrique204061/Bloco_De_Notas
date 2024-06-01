package Modelo

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("tabela_tarefas")
class Tarefa(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val titulo: String,
    val descricao: String,
    val data: Long,
    var isChecked: Boolean = false
)