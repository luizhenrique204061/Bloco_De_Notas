package com.olamundo.blocodenotas

import Adapter.TarefasAdapter
import DB.DB
import Modelo.Tarefa
import Room.AppDataBase
import Room.TarefaDao
import android.app.AlertDialog
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.olamundo.blocodenotas.databinding.ActivityCriarTarefaBinding
import com.olamundo.blocodenotas.databinding.DialogExclusaoActivityCriarTarefaBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

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
    val db = DB()
    lateinit var mAdview: AdView

    override fun onCreate(savedInstanceState: Bundle?) {
        carregarLocalidade()
        binding = ActivityCriarTarefaBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        loadTheme()

        titulo = binding.tituloTarefa.text.toString()

        bancoDeDados = AppDataBase.getInstance(this).TarefaDao()

        adapterTarefas = TarefasAdapter(this, listaTarefas)
        val recyclerView = binding.recyclerViewTarefas
        recyclerView.adapter = adapterTarefas

        carregarAnuncioBanner()

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
                val riscado = descricao.startsWith("~~") && descricao.endsWith("~~")
                val descricaoLimpa = if (riscado) descricao.removeSurrounding("~~") else descricao
                adicionarTarefa(descricaoLimpa, riscado)
            }
        }

        // Definindo a cor de seleção do texto para verde
        val greenColor = getColor(R.color.verde_claro) // Certifique-se de ter definido a cor verde no colors.xml
        binding.tituloTarefa.highlightColor = greenColor
        binding.descricaoTarefa.highlightColor = greenColor

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

    private fun carregarAnuncioBanner() {
        //Anúncio do Tipo Banner

        MobileAds.initialize(this)
        mAdview = binding.adview
        val adRequest = AdRequest.Builder().build()
        Log.i("Meu App", "Antes de carregar o anúncio")
        mAdview.loadAd(adRequest)
    }

    private suspend fun salvar() {
        titulo = binding.tituloTarefa.text.toString()
        atualizarTextoDoEditText() // Atualiza o texto antes de salvar
        if (titulo.isNotEmpty()) {
            criarTarefas(tarefaId, titulo, textoDoEditText, hora)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@CriarTarefa, getString(R.string.tarefa_salva_com_sucesso), Toast.LENGTH_SHORT).show()
            }
            retornar()
        } else {
            Snackbar.make(binding.root, getString(R.string.preencha_todos_os_campos), Snackbar.LENGTH_SHORT).apply {
                this.setBackgroundTint(Color.RED)
                this.setTextColor(Color.WHITE)
                this.show()
            }
        }
    }

    private suspend fun deletar() {
        titulo = binding.tituloTarefa.text.toString()
        val descricao = binding.descricaoTarefa.text.toString()

        if (titulo.isEmpty() && textoDoEditText.isEmpty() && descricao.isEmpty()) {
            retornar()
        } else {
            withContext(Dispatchers.Main) {
                val dialogBinding = DialogExclusaoActivityCriarTarefaBinding.inflate(layoutInflater)
                val exibirDialog = AlertDialog.Builder(this@CriarTarefa)
                    .setView(dialogBinding.root)
                    .setCancelable(false)
                    .create() // Cria o AlertDialog, mas não o mostra ainda

                // Configura o fundo do diálogo como transparente
                exibirDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

                exibirDialog.show() // Mostra o AlertDialog

                dialogBinding.botaoCancelar.setOnClickListener {
                    exibirDialog.dismiss()
                }

                dialogBinding.botaoProsseguir.setOnClickListener {
                    scope.launch {
                        val currentUser = FirebaseAuth.getInstance().currentUser

                        currentUser?.let {
                            val excluirTarefaFirebase = db.excluirTarefasUsuario(tarefaId)
                            if (excluirTarefaFirebase) {
                                // Se excluiu com sucesso do Firebase, exclui do Room
                                bancoDeDados.remover(tarefaId)
                            }
                        } ?: run {
                            bancoDeDados.remover(tarefaId)
                        }

                        withContext(Dispatchers.Main) {
                            retornar()
                        }
                    }
                }
            }
        }
    }

    private fun retornar() {
        Intent(this, MainActivity::class.java).apply {
            putExtra("navigate_to_fragment", "FragmentoTarefas")
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(this)
            finish()
        }
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

    private fun adicionarTarefa(descricao: String, riscado: Boolean = false) {
        titulo = binding.tituloTarefa.text.toString()
        // Adicionar nova tarefa à lista
        val novaTarefa = Tarefa(tarefaId, titulo, descricao, hora, false, riscado)
        listaTarefas.add(novaTarefa)
        // Notificar o adapter da inserção de um novo item
        adapterTarefas.notifyItemInserted(listaTarefas.size - 1)
        Log.i("CriarTarefa", "Tarefa adicionada: ${novaTarefa.descricao}")
    }

    private fun atualizarTextoDoEditText() {
        val tarefasText = listaTarefas.joinToString(", ") {
            if (it.isRiscado) "~~${it.descricao}~~" else it.descricao
        }
        textoDoEditText = tarefasText
        Log.i("CriarTarefaCapturado", "Texto no EditText: $textoDoEditText")
    }

    private fun selecionarIdioma(linguagem: String) {
        val localidade = Locale(linguagem)
        Locale.setDefault(localidade)

        // Obter o objeto Configuration da atividade atual
        val configuration = resources.configuration

        // Configurar a localidade para a Configuration
        configuration.setLocale(localidade)

        // Atualizar a Configuration na atividade atual
        resources.updateConfiguration(configuration, resources.displayMetrics)

    }

    private fun carregarLocalidade() {
        val preferences = getSharedPreferences("config_linguagens", MODE_PRIVATE)
        val localidadeDoDispositivo = Locale.getDefault().language
        val linguagem = preferences.getString("minha_linguagem", localidadeDoDispositivo)
        if (linguagem != null) {
            selecionarIdioma(linguagem)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        titulo = binding.tituloTarefa.text.toString()
        val descricao = binding.descricaoTarefa.text.toString()

        scope.launch {
            if (titulo.isNotEmpty() && descricao.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    adicionarTarefa(descricao)
                    atualizarTextoDoEditText() // Atualiza o texto antes de salvar
                }
                criarTarefas(tarefaId, titulo, textoDoEditText, hora)
                withContext(Dispatchers.Main) {
                    finish()
                }
            } else if (listaTarefas.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    if (descricao.isNotBlank()) {
                        adicionarTarefa(descricao)
                    }
                    atualizarTextoDoEditText()
                }
                criarTarefas(tarefaId, titulo, textoDoEditText, hora)
            }
        }
    }


    private fun loadTheme() {
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        when (currentNightMode) {
            Configuration.UI_MODE_NIGHT_YES -> applyDarkTheme()
            Configuration.UI_MODE_NIGHT_NO -> applyLightTheme()
        }
    }

    private fun applyDarkTheme() {
        binding.adiconar.setBackgroundResource(R.drawable.ic_add_branco)
    }

    private fun applyLightTheme() {
        binding.adiconar.setBackgroundResource(R.drawable.ic_add_preto)
    }
}