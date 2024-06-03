package Adapter

import Modelo.NotasProtegidas
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.olamundo.blocodenotas.CriarAnotacaoProtegida
import com.olamundo.blocodenotas.CriarNota
import com.olamundo.blocodenotas.R
import com.olamundo.blocodenotas.databinding.NotasItemBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotasProtegidasAdapter(
    val context: Context,
    val listaNotasAdapterProtegidas: MutableList<NotasProtegidas> = mutableListOf(),
    val listener: OnItemSelectedListener
): RecyclerView.Adapter<NotasProtegidasAdapter.ViewHolder>() {

    private var longClick = false

    fun isLongClick(): Boolean {
        return longClick
    }
    inner class ViewHolder(private val binding: NotasItemBinding): RecyclerView.ViewHolder(binding.root) {

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

        fun vincula(notasProtegidas: NotasProtegidas) {
            val titulo = binding.titulo
            titulo.text = notasProtegidas.titulo

            val descricao = binding.descricao
            descricao.text = notasProtegidas.descricao

            val data = binding.data
            val formatoDataHoraString = context.getString(R.string.formato_data_hora)
            val formatoDataHora = SimpleDateFormat(formatoDataHoraString, Locale.getDefault())
            val dataHoraFormatada = formatoDataHora.format(Date(notasProtegidas.data))
            data.text = context.getString(R.string.edicao_em, dataHoraFormatada)

            // Definindo visibilidade do Checkbox baseado no modo de seleção
            if (selecaoAtiva) {
                binding.checkbox.visibility = View.VISIBLE
                binding.checkbox.isChecked = notasProtegidas.isChecked
            } else {
                binding.checkbox.visibility = View.GONE
            }

            // Configurar o clique do item
            binding.root.setOnClickListener {
                if (selecaoAtiva) {
                    toggleItemSelection(adapterPosition) // Se o modo de seleção estiver ativado, alternar seleção
                } else {
                    onItemClicked(notasProtegidas)
                }
            }
        }

        private fun toggleItemSelection(position: Int) {
            if (position != RecyclerView.NO_POSITION && position >= 0 && position < listaNotasAdapterProtegidas.size) {
                listaNotasAdapterProtegidas[position].isChecked = !listaNotasAdapterProtegidas[position].isChecked
                val selectedCount = listaNotasAdapterProtegidas.count { it.isChecked }
                listener.onItemSelected(selectedCount)
                listener.updateSelectedItemCount(selectedCount)
                notifyItemChanged(position)
            }
        }

        private fun toggleSelectionMode(position: Int) {
            if (position != RecyclerView.NO_POSITION && position >= 0 && position < listaNotasAdapterProtegidas.size) {
                selecaoAtiva = true
                listaNotasAdapterProtegidas[position].isChecked = !listaNotasAdapterProtegidas[position].isChecked
                listener.onItemLongClicked()
                val selectedCount = listaNotasAdapterProtegidas.count { it.isChecked }
                listener.onItemSelected(selectedCount)
                listener.updateSelectedItemCount(selectedCount)
                notifyDataSetChanged()
            }
        }


        private fun onItemClicked(notas: NotasProtegidas) {
            if (!selecaoAtiva) {
                Log.i("Clicando", "Clique")
                val intent = Intent(context, CriarAnotacaoProtegida::class.java).apply {
                    putExtra("titulo", notas.titulo)
                    putExtra("descricao", notas.descricao)
                    putExtra("data", notas.data)
                    putExtra("usuarioId", notas.usuarioId)
                    putExtra("anotacaoId", notas.anotacaoId)
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
        return listaNotasAdapterProtegidas.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itens = listaNotasAdapterProtegidas[position]
        holder.vincula(itens)
    }

    // Método para desativar o modo de seleção
    fun desativarModoSelecao() {
        // Desmarcar todos os checkboxes
        for (nota in listaNotasAdapterProtegidas) {
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

    interface OnItemSelectedListener {
        fun onItemSelected(selectedItemCount: Int)
        fun onItemLongClicked()
        fun updateSelectedItemCount(selectedItemCount: Int) // Novo método para contar itens selecionados
    }
}