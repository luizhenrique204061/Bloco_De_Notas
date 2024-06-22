package Adapter

import Modelo.Tarefa
import android.content.Context
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.olamundo.blocodenotas.databinding.TarefaItemBinding

class TarefasAdapter(
    val context: Context,
    val listaTarefas: MutableList<Tarefa> = mutableListOf()
): RecyclerView.Adapter<TarefasAdapter.ViewHolder>() {
    inner class ViewHolder(private val binding: TarefaItemBinding): RecyclerView.ViewHolder(binding.root) {

        fun vincula(tarefa: Tarefa) {
            val descricao = binding.descricaoLista
            descricao.text = tarefa.descricao

            // Aplicar o estilo riscado se a tarefa estiver riscada
            if (tarefa.isRiscado) {
                descricao.paintFlags = descricao.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                descricao.paintFlags = descricao.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            descricao.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    // Alternar o estado riscado
                    tarefa.isRiscado = !tarefa.isRiscado
                    notifyItemChanged(position)
                }
            }

            binding.removerTarefa.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    removerTarefa(position)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val item = TarefaItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(item)
    }

    override fun getItemCount(): Int {
        return listaTarefas.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itens = listaTarefas[position]
        holder.vincula(itens)
    }


    private fun removerTarefa(position: Int) {
        listaTarefas.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, itemCount)
    }
}