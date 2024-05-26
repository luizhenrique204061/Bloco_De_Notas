package com.olamundo.blocodenotas

import Adapter.TarefasAdapter
import Modelo.Tarefa
import Room.AppDataBase
import Room.TarefaDao
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.olamundo.blocodenotas.databinding.ActivityCriarTarefaBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class CriarTarefa : AppCompatActivity() {
    private lateinit var binding: ActivityCriarTarefaBinding
    private lateinit var adapterTarefas: TarefasAdapter
    val listaTarefas: MutableList<Tarefa> = mutableListOf()
    private lateinit var bancoDeDados: TarefaDao
    private lateinit var titulo: String
    private var textoDoEditText = ""
    val hora = System.currentTimeMillis()
    var tarefaId: Long = 0
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

        val id = intent.getLongExtra("id", 0L)
        val recuperarTitulo = intent.getStringExtra("titulo")
        val recuperarDescricao = intent.getStringExtra("descricao")

        if (id != 0L && recuperarTitulo != null && recuperarDescricao != null) {
            Log.i("RecuperarTarefa", "Id: $id")
            Log.i("RecuperarTarefa", "Título: $recuperarTitulo")
            Log.i("RecuperarTarefa", "Descriçao: $recuperarDescricao")

           // titulo = recuperarTitulo // Inicializa a variável título

            tarefaId = id

            titulo = binding.tituloTarefa.setText(recuperarTitulo).toString()

            // Dividir a descrição em itens e adicioná-los à lista de tarefas
            val itensDescricao = recuperarDescricao.split(",").map { it.trim() }
            itensDescricao.forEach { descricao ->
                adicionarTarefa(descricao)
            }
        }

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
                compartilharTarefa()
            }

            R.id.menu_remover -> {
                scope.launch {
                    deletar()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private suspend fun salvar() {
        titulo = binding.tituloTarefa.text.toString()
        atualizarTextoDoEditText() // Atualiza o texto antes de salvar
        criarTarefas(tarefaId, titulo, textoDoEditText, hora)
        finish()
    }

    private suspend fun deletar() {
        bancoDeDados.remover(tarefaId)
        finish()
    }

    private suspend fun criarTarefas(id: Long, titulo: String, descricao: String, hora: Long) {
        val tarefa = Tarefa(id, titulo, descricao, hora)
        bancoDeDados.salvarTarefa(tarefa)
    }

    private fun compartilharTarefa() {
        titulo = binding.tituloTarefa.text.toString()
        atualizarTextoDoEditText()

        val txtDados = "${titulo}\n${textoDoEditText}"

        val nomeArquivo = "$titulo.txt"
        val arquivo = File(filesDir, nomeArquivo)

        arquivo.writeText(txtDados)

        val uri = FileProvider.getUriForFile(
            this@CriarTarefa,
            "com.olamundo.blocodenotas.fileprovider",
            arquivo
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Confira a nota: $titulo")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(intent, "Compartilhar nota via"))
    }

    private fun adicionarTarefa(descricao: String) {
        titulo = binding.tituloTarefa.text.toString()
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