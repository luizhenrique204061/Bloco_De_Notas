package Adapter

import Modelo.Tarefa
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.olamundo.blocodenotas.CriarTarefa
import com.olamundo.blocodenotas.R
import com.olamundo.blocodenotas.databinding.TarefaItemTelaPrincipalBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TarefasAdapterTelaPrincipal(
    val context: Context,
    val listaTarefasTelaPrincipal: MutableList<Tarefa> = mutableListOf(),
    val listener: OnItemSelectedListener
): RecyclerView.Adapter<TarefasAdapterTelaPrincipal.ViewHolder>() {


    private var longClick = false

    fun isLongClick(): Boolean {
        return longClick
    }
    inner class ViewHolder(private val binding: TarefaItemTelaPrincipalBinding): RecyclerView.ViewHolder(binding.root) {


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

        fun vincula(tarefa: Tarefa) {
            val titulo = binding.titulo
            titulo.text = tarefa.titulo

            val descricao = binding.descricao
            descricao.text = tarefa.descricao

            val data = binding.data
            val formatoDataHoraString = context.getString(R.string.formato_data_hora)
            val formatoDataHora = SimpleDateFormat(formatoDataHoraString, Locale.getDefault())
            val dataHoraFormatada = formatoDataHora.format(Date(tarefa.data))
            data.text = context.getString(R.string.edicao_em, dataHoraFormatada)

            // Definindo visibilidade do Checkbox baseado no modo de seleção
            if (selecaoAtiva) {
                binding.checkbox.visibility = View.VISIBLE
                binding.checkbox.isChecked = tarefa.isChecked
            } else {
                binding.checkbox.visibility = View.GONE
            }

            // Configurar o clique do item
            binding.root.setOnClickListener {
                if (selecaoAtiva) {
                    toggleItemSelection(adapterPosition) // Se o modo de seleção estiver ativado, alternar seleção
                } else {
                    onItemClicked(tarefa)
                }
            }
        }

        private fun toggleItemSelection(position: Int) {
            if (position != RecyclerView.NO_POSITION && position >= 0 && position < listaTarefasTelaPrincipal.size) {
                listaTarefasTelaPrincipal[position].isChecked = !listaTarefasTelaPrincipal[position].isChecked
                val selectedCount = listaTarefasTelaPrincipal.count { it.isChecked }
                listener.onItemSelected(selectedCount)
                listener.updateSelectedItemCount(selectedCount)
                notifyItemChanged(position)
            }
        }

        private fun toggleSelectionMode(position: Int) {
            if (position != RecyclerView.NO_POSITION && position >= 0 && position < listaTarefasTelaPrincipal.size) {
                selecaoAtiva = true
                listaTarefasTelaPrincipal[position].isChecked = !listaTarefasTelaPrincipal[position].isChecked
                listener.onItemLongClicked()
                val selectedCount = listaTarefasTelaPrincipal.count { it.isChecked }
                listener.onItemSelected(selectedCount)
                listener.updateSelectedItemCount(selectedCount)
                notifyDataSetChanged()
            }
        }


        private fun onItemClicked(tarefa: Tarefa) {
            if (!selecaoAtiva) {
                Log.i("Clicando", "Clique")
                val intent = Intent(context, CriarTarefa::class.java).apply {
                    putExtra("id", tarefa.id)
                    putExtra("titulo", tarefa.titulo)
                    putExtra("descricao", tarefa.descricao)
                    putExtra("data", tarefa.data)
                }
                context.startActivity(intent)
            }
        }
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val item = TarefaItemTelaPrincipalBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(item)
    }

    override fun getItemCount(): Int {
        return listaTarefasTelaPrincipal.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itens = listaTarefasTelaPrincipal[position]
        holder.vincula(itens)

    }

    // Método para desativar o modo de seleção
    fun desativarModoSelecao() {
        // Desmarcar todos os checkboxes
        for (nota in listaTarefasTelaPrincipal) {
            nota.isChecked = false
        }
        // Desativar o modo de seleção
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
        fun updateSelectedItemCount(selectedItemCount: Int) // Novo método para contar itens selecionados
    }
}