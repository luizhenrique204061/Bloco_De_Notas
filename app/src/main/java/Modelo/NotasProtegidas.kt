package Modelo

import java.io.Serializable

class NotasProtegidas(
    val titulo: String = "",
    val descricao: String = "",
    val data: Long = 0,
    val usuarioId: String = "",
    val anotacaoId: String = "",
    var isChecked: Boolean = false
): Serializable {
    constructor(): this("", "", 0, "", "", false)
}