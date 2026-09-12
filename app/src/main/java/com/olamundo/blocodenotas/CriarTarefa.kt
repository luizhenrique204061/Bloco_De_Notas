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
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
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
    private var id: Long? = null
    private var recuperarTitulo: String? = null
    private var recuperarDescricao: String? = null
    val scope = CoroutineScope(Dispatchers.IO)
    val db = DB()
    lateinit var mAdview: AdView

    override fun onCreate(savedInstanceState: Bundle?) {
        carregarLocalidade()
        binding = ActivityCriarTarefaBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val isModoEscuro = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        // 1. Sincroniza a barra de status com o tema ativo
        window.statusBarColor = if (isModoEscuro) {
            ContextCompat.getColor(this, R.color.black)
        } else {
            ContextCompat.getColor(this, R.color.white)
        }

        // 2. Controla o contraste dos ícones da barra (escuros no claro, brancos no escuro)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isModoEscuro
            isAppearanceLightNavigationBars = !isModoEscuro
        }

        // 3. Afasta a Toolbar do recorte da câmera e o rodapé da barra de gestos
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 4. Manipulador retrocompatível para botão e gestos de voltar
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val tituloAtual = binding.tituloTarefa.text.toString()
                val descricaoAtual = binding.descricaoTarefa.text.toString()
                atualizarTextoDoEditText()

                val houveAlteracao = tituloAtual != recuperarTitulo || textoDoEditText != recuperarDescricao

                if (houveAlteracao) {
                    scope.launch {
                        if (tituloAtual.isNotEmpty() || textoDoEditText.isNotEmpty()) {
                            atualizarTarefa(tarefaId, tituloAtual, textoDoEditText, hora)
                        } else if (descricaoAtual.isNotEmpty()) {
                            atualizarTarefa(tarefaId, descricaoAtual, descricaoAtual, hora)
                        } else if (listaTarefas.isNotEmpty()) {
                            atualizarTarefa(tarefaId, tituloAtual, descricaoAtual, hora)
                        }
                        withContext(Dispatchers.Main) {
                            finish()
                        }
                    }
                } else {
                    finish()
                }
            }
        })

        setSupportActionBar(binding.toolbar)

        loadTheme()

        titulo = binding.tituloTarefa.text.toString()

        bancoDeDados = AppDataBase.getInstance(this).TarefaDao()

        adapterTarefas = TarefasAdapter(this, listaTarefas)
        val recyclerView = binding.recyclerViewTarefas
        recyclerView.adapter = adapterTarefas

        id = intent?.getLongExtra("id", 0L)
        recuperarTitulo = intent?.getStringExtra("titulo")
        recuperarDescricao = intent?.getStringExtra("descricao")

        if (id != 0L && recuperarTitulo != null && recuperarDescricao != null) {
            tarefaId = id!!
            titulo = binding.tituloTarefa.setText(recuperarTitulo).toString()

            val itensDescricao = recuperarDescricao!!.split(",").map { it.trim() }
            itensDescricao.forEach { descricao ->
                val riscado = descricao.startsWith("~~") && descricao.endsWith("~~")
                val descricaoLimpa = if (riscado) descricao.removeSurrounding("~~") else descricao
                adicionarTarefa(descricaoLimpa, riscado)
            }
        }

        val greenColor = getColor(R.color.verde_claro)
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
                scope.launch { salvar() }
            }
            R.id.menu_compartilhar -> {
                compartilharTarefa()
            }
            R.id.menu_remover -> {
                scope.launch { deletar() }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun carregarAnuncioBanner() {
        MobileAds.initialize(this)
        mAdview = binding.adview
        val adRequest = AdRequest.Builder().build()
        mAdview.loadAd(adRequest)
    }

    private suspend fun salvar() {
        titulo = binding.tituloTarefa.text.toString()
        atualizarTextoDoEditText()
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
                    .create()

                exibirDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                exibirDialog.show()

                dialogBinding.botaoCancelar.setOnClickListener {
                    exibirDialog.dismiss()
                }

                dialogBinding.botaoProsseguir.setOnClickListener {
                    scope.launch {
                        val currentUser = FirebaseAuth.getInstance().currentUser

                        currentUser?.let {
                            val excluirTarefaFirebase = db.excluirTarefasUsuario(tarefaId)
                            if (excluirTarefaFirebase) {
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
        val novaTarefa = Tarefa(tarefaId, titulo, descricao, hora, false, riscado)
        listaTarefas.add(novaTarefa)
        adapterTarefas.notifyItemInserted(listaTarefas.size - 1)
    }

    private fun atualizarTextoDoEditText() {
        val tarefasText = listaTarefas.joinToString(", ") {
            if (it.isRiscado) "~~${it.descricao}~~" else it.descricao
        }
        textoDoEditText = tarefasText
    }

    private fun selecionarIdioma(linguagem: String) {
        val localidade = Locale(linguagem)
        Locale.setDefault(localidade)

        val configuration = resources.configuration
        configuration.setLocale(localidade)
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

    suspend fun atualizarTarefa(id: Long, titulo: String, descricao: String, hora: Long) {
        val tarefas = Tarefa(id, titulo, descricao, hora)
        bancoDeDados.atualizar(tarefas)
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