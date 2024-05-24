package com.olamundo.blocodenotas

import Adapter.TarefasAdapter
import Modelo.Tarefa
import Room.AppDataBase
import Room.TarefaDao
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.olamundo.blocodenotas.databinding.ActivityCriarTarefaBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CriarTarefa : AppCompatActivity() {
    private lateinit var binding: ActivityCriarTarefaBinding
    private lateinit var adapterTarefas: TarefasAdapter
    val listaTarefas: MutableList<Tarefa> = mutableListOf()
    private lateinit var bancoDeDados: TarefaDao
    private lateinit var titulo: String
    private lateinit var textoDoEditText: String
    val hora = System.currentTimeMillis()
    val tarefaId: Long = 0
    val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityCriarTarefaBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        bancoDeDados = AppDataBase.getInstance(this).TarefaDao()

        adapterTarefas = TarefasAdapter(this, listaTarefas)
        val recyclerView = binding.recyclerViewTarefas

        recyclerView.adapter = adapterTarefas

        titulo = binding.tituloTarefa.text.toString()

        binding.adiconar.setOnClickListener {
            val descricao = binding.descricaoTarefa.text.toString()
            if (descricao.isNotBlank()) {
                adicionarTarefa(descricao)
                binding.descricaoTarefa.text.clear()
                atualizarTextoDoEditText()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_salvar_compartilhar_remover, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_salvar -> {
                scope.launch {
                    salvar()
                }
            }

            R.id.menu_compartilhar -> {
                // Implementar funcionalidade de compartilhar
            }

            R.id.menu_remover -> {
                // Implementar funcionalidade de remover
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private suspend fun salvar() {
        titulo = binding.tituloTarefa.text.toString()
        criarTarefas(tarefaId, titulo, textoDoEditText, hora)
    }

    private suspend fun criarTarefas(id: Long, titulo: String, descricao: String, hora: Long) {

        val tarefa = Tarefa(id, titulo, descricao, hora)
        bancoDeDados.salvarTarefa(tarefa)

    }

    private fun adicionarTarefa(descricao: String) {
        // Adicionar nova tarefa à lista
        val novaTarefa = Tarefa(tarefaId, titulo, descricao, hora)
        listaTarefas.add(novaTarefa)
        // Notificar o adapter da inserção de um novo item
        adapterTarefas.notifyItemInserted(listaTarefas.size - 1)
        Log.i("CriarTarefa", "Tarefa adicionada: ${novaTarefa.descricao}")
    }

    private fun atualizarTextoDoEditText() {
        val tarefasText = listaTarefas.joinToString(", ") { it.descricao }
        textoDoEditText = tarefasText
        Log.i("CriarTarefaCapturado", "Texto no EditText: $textoDoEditText")
    }
}