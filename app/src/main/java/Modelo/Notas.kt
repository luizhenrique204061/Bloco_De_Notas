package Modelo

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "tabela_notas")
data class Notas(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val titulo: String = "",
    val descricao: String = "",
    val data: Long = 0,
    var isChecked: Boolean = false
) : Serializable
/* //Código alternativo para serializar, sem o construtor secundário.
:Serializable {
    constructor() : this(0, "", "", 0, false)
}

 */