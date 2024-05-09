package Adapter

import Modelo.Notas
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.olamundo.blocodenotas.CriarNota
import com.olamundo.blocodenotas.R
import com.olamundo.blocodenotas.databinding.NotasItemBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ListaNotasAdapter(
    val context: Context,
    val listaNotas: MutableList<Notas> = mutableListOf(),
    val listener: OnItemSelectedListener
) : RecyclerView.Adapter<ListaNotasAdapter.ViewHolder>() {

    private var longClick = false

    fun isLongClick(): Boolean {
        return longClick
    }

    inner class ViewHolder(private val binding: NotasItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            // Configurar o clique longo para ativar o modo de seleção
            binding.root.setOnLongClickListener {
                longClick = true // Define que houve um clique longo
                toggleSelectionMode(adapterPosition)
                true
            }

            // Configurar o clique do checkbox
            binding.checkbox.setOnClickListener {
                toggleItemSelection(adapterPosition)
            }
        }

        fun vincula(notas: Notas, position: Int) {
            val titulo = binding.titulo
            titulo.text = notas.titulo

            val descricao = binding.descricao
            descricao.text = notas.descricao

            val data = binding.data
            val formatoDataHoraString = context.getString(R.string.formato_data_hora)
            val formatoDataHora = SimpleDateFormat(formatoDataHoraString, Locale.getDefault())
            val dataHoraFormatada = formatoDataHora.format(Date(notas.data))
            data.text = context.getString(R.string.edicao_em, dataHoraFormatada)

            // Definindo visibilidade do Checkbox baseado no modo de seleção
            if (selecaoAtiva) {
                binding.checkbox.visibility = View.VISIBLE
                // Marcar o checkbox apenas no item clicado longamente
                binding.checkbox.isChecked = notas.isChecked
            } else {
                // Exibir checkbox sem marcação
                binding.checkbox.visibility = View.GONE
            }

            // Configurar o clique do item
            binding.root.setOnClickListener {
                if (selecaoAtiva) {
                    toggleItemSelection(adapterPosition) // Se o modo de seleção estiver ativado, alternar seleção
                } else {
                    // Se não estiver em modo de seleção, lidar com o clique normal
                    onItemClicked(notas)
                }
            }
        }

        private fun toggleItemSelection(position: Int) {
            if (position != RecyclerView.NO_POSITION) {
                // Trocar o estado de marcação do item clicado
                listaNotas[position].isChecked = !listaNotas[position].isChecked
                // Notificar o listener sobre a mudança no número de itens selecionados
                listener.onItemSelected(listaNotas.count { it.isChecked })
                // Atualizar a exibição do item clicado
                notifyItemChanged(position)
            }
        }

        private fun toggleSelectionMode(position: Int) {
            selecaoAtiva = true
            // Desmarcar todos os itens antes de marcar o selecionado
            listaNotas.forEach { it.isChecked = false }
            // Marcar o item clicado longamente
            listaNotas[position].isChecked = true
            listener.onItemLongClicked()
            notifyDataSetChanged()
        }

        private fun onItemClicked(notas: Notas) {
            if (!selecaoAtiva) {
                Log.i("Clicando", "Clique")
                val intent = Intent(context, CriarNota::class.java).apply {
                    putExtra("id", notas.id)
                    putExtra("titulo", notas.titulo)
                    putExtra("descricao", notas.descricao)
                    putExtra("data", notas.data)
                }
                context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val item = NotasItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(item)
    }

    override fun getItemCount(): Int {
        return listaNotas.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = listaNotas[position]
        holder.vincula(item, position)
    }

    // Método para desativar o modo de seleção
    fun desativarModoSelecao() {
        selecaoAtiva = false
        notifyDataSetChanged()
    }

    // Método para verificar se o modo de seleção está ativo
    fun isSelecaoAtiva(): Boolean {
        return selecaoAtiva
    }

    // Indica se o modo de seleção está ativo ou não
    private var selecaoAtiva = false

    // Interface para comunicação de eventos de seleção
    interface OnItemSelectedListener {
        fun onItemSelected(selectedItemCount: Int)
        fun onItemLongClicked()
    }
}