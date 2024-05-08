package Adapter

import Modelo.Notas
import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.olamundo.blocodenotas.R
import com.olamundo.blocodenotas.databinding.NotasItemBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ListaNotasAdapter(
    val context: Context,
    val listaNotas: MutableList<Notas> = mutableListOf()
): RecyclerView.Adapter<ListaNotasAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: NotasItemBinding): RecyclerView.ViewHolder(binding.root) {


        fun vincula(notas: Notas) {
            val titulo = binding.titulo
            titulo.text = notas.titulo

            val descricao = binding.descricao
            descricao.text = notas.descricao

            val data = binding.data
            val formatoDataHoraString = context.getString(R.string.formato_data_hora)
            val formatoDataHora = SimpleDateFormat(formatoDataHoraString, Locale.getDefault())
            val dataHoraFormatada = formatoDataHora.format(Date(notas.data))
            data.text = context.getString(R.string.edicao_em, dataHoraFormatada)

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
        val itens = listaNotas[position]
        holder.vincula(itens)
    }
}