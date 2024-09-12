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
    val context: Context, // Contexto da aplicação.
    val listaNotas: MutableList<Notas> = mutableListOf(), // Lista de notas que será exibida no RecyclerView.
    val listener: OnItemSelectedListener // Listener para gerenciar eventos de clique.
) : RecyclerView.Adapter<ListaNotasAdapter.ViewHolder>() {

    private var longClick = false // Variável que indica se houve um clique longo.

    // Retorna se houve um clique longo.
    fun isLongClick(): Boolean {
        return longClick
    }

    // Classe interna ViewHolder que contém a lógica para cada item da lista.
    inner class ViewHolder(private val binding: NotasItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            // Configura o clique longo no item da lista para ativar o modo de seleção.
            binding.root.setOnLongClickListener {
                longClick = true // Marca que houve um clique longo.
                toggleSelectionMode(adapterPosition) // Alterna o modo de seleção ao fazer o clique longo.
                true
            }

            // Configura o clique do checkbox.
            binding.checkbox.setOnClickListener {
                toggleItemSelection(adapterPosition) // Alterna a seleção do item ao clicar no checkbox.
            }
        }

        // Vincula os dados da nota ao layout.
        fun vincula(notas: Notas) {
            val titulo = binding.titulo
            titulo.text = notas.titulo // Define o título da nota.

            val descricao = binding.descricao
            descricao.text = notas.descricao // Define a descrição da nota.

            val data = binding.data
            val formatoDataHoraString = context.getString(R.string.formato_data_hora) // Pega o formato da data da string de recursos.
            val formatoDataHora = SimpleDateFormat(formatoDataHoraString, Locale.getDefault()) // Formata a data conforme a localidade.
            val dataHoraFormatada = formatoDataHora.format(Date(notas.data)) // Formata a data da nota.
            data.text = context.getString(R.string.edicao_em, dataHoraFormatada) // Exibe a data formatada.

            // Define a visibilidade do checkbox com base no modo de seleção.
            if (selecaoAtiva) {
                binding.checkbox.visibility = View.VISIBLE // Exibe o checkbox se o modo de seleção estiver ativo.
                binding.checkbox.isChecked = notas.isChecked // Define se o checkbox está marcado ou não.
            } else {
                binding.checkbox.visibility = View.GONE // Esconde o checkbox se o modo de seleção não estiver ativo.
            }

            // Configura o clique no item da lista.
            binding.root.setOnClickListener {
                if (selecaoAtiva) {
                    toggleItemSelection(adapterPosition) // Se o modo de seleção estiver ativo, alterna a seleção do item.
                } else {
                    onItemClicked(notas) // Caso contrário, abre a nota clicada.
                }
            }
        }

        // Alterna a seleção de um item na lista.
        private fun toggleItemSelection(position: Int) {
            if (position != RecyclerView.NO_POSITION && position >= 0 && position < listaNotas.size) {
                listaNotas[position].isChecked = !listaNotas[position].isChecked // Alterna o estado selecionado.
                val selectedCount = listaNotas.count { it.isChecked } // Conta quantos itens estão selecionados.
                listener.onItemSelected(selectedCount) // Notifica o listener sobre a mudança na contagem de itens selecionados.
                listener.updateSelectedItemCount(selectedCount) // Atualiza a contagem de itens selecionados.
                notifyItemChanged(position) // Atualiza o item visualmente.
            }
        }

        // Alterna o modo de seleção (ativado por um clique longo).
        private fun toggleSelectionMode(position: Int) {
            if (position != RecyclerView.NO_POSITION && position >= 0 && position < listaNotas.size) {
                selecaoAtiva = true // Ativa o modo de seleção.
                listaNotas[position].isChecked = !listaNotas[position].isChecked // Alterna o estado do item clicado.
                listener.onItemLongClicked() // Notifica o listener que o clique longo ocorreu.
                val selectedCount = listaNotas.count { it.isChecked } // Conta quantos itens estão selecionados.
                listener.onItemSelected(selectedCount) // Notifica o listener sobre a contagem de itens selecionados.
                listener.updateSelectedItemCount(selectedCount) // Atualiza a contagem de itens selecionados.
                notifyDataSetChanged() // Atualiza todos os itens visualmente.
            }
        }

        // Executa a ação de abrir o item (quando não está no modo de seleção).
        private fun onItemClicked(notas: Notas) {
            if (!selecaoAtiva) {
                Log.i("Clicando", "Clique") // Log para indicar o clique.
                val intent = Intent(context, CriarNota::class.java).apply {
                    // Envia os dados da nota clicada para a atividade CriarNota.
                    putExtra("id", notas.id)
                    putExtra("titulo", notas.titulo)
                    putExtra("descricao", notas.descricao)
                    putExtra("data", notas.data)
                }
                context.startActivity(intent) // Inicia a atividade de edição da nota.
            }
        }
    }

    // Cria o ViewHolder que contém o layout do item.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val item = NotasItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(item) // Retorna o ViewHolder criado.
    }

    // Retorna a quantidade de itens na lista.
    override fun getItemCount(): Int {
        return listaNotas.size
    }

    // Vincula os dados do item atual ao ViewHolder.
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = listaNotas[position]
        holder.vincula(item) // Vincula o item (nota) ao ViewHolder.
    }

    // Método para desativar o modo de seleção.
    fun desativarModoSelecao() {
        // Desmarca todos os itens selecionados.
        for (nota in listaNotas) {
            nota.isChecked = false
        }
        selecaoAtiva = false // Desativa o modo de seleção.
        notifyDataSetChanged() // Atualiza todos os itens visualmente.
    }

    // Verifica se o modo de seleção está ativo.
    fun isSelecaoAtiva(): Boolean {
        return selecaoAtiva
    }

    // Variável que indica se o modo de seleção está ativo.
    private var selecaoAtiva = false

    // Interface para comunicação de eventos de seleção com a atividade ou fragmento.
    interface OnItemSelectedListener {
        fun onItemSelected(selectedItemCount: Int) // Método chamado quando o número de itens selecionados muda.
        fun onItemLongClicked() // Método chamado quando um item é clicado longamente.
        fun updateSelectedItemCount(selectedItemCount: Int) // Método para atualizar o número de itens selecionados.
    }
}