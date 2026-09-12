package com.olamundo.blocodenotas

import DB.DB
import Modelo.Notas
import Room.AppDataBase
import Room.NotaDao
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.olamundo.blocodenotas.databinding.ActivityCriarNotaBinding
import com.olamundo.blocodenotas.databinding.DialogExclusaoActivityCriarNotaBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale


class CriarNota : AppCompatActivity() {
    var notaId: Long = 0
    val hora = System.currentTimeMillis()
    private lateinit var bancoDeDados: NotaDao
    val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var binding: ActivityCriarNotaBinding
    private lateinit var titulo: String
    val db = DB()
    private lateinit var descricao: String
    private var recuperarTitulo: String? = null
    private var recuperarDescricao: String? = null
    private var id: Long? = null
    lateinit var mAdview: AdView

    override fun onCreate(savedInstanceState: Bundle?) {
        carregarLocalidade()
        binding = ActivityCriarNotaBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val isModoEscuro = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        // 1. Sincroniza a barra de status com o tema ativo
        window.statusBarColor = if (isModoEscuro) {
            ContextCompat.getColor(this, R.color.black)
        } else {
            ContextCompat.getColor(this, R.color.white)
        }

        // 2. Controla o contraste dos ícones do sistema (escuros no claro, brancos no escuro)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isModoEscuro
            isAppearanceLightNavigationBars = !isModoEscuro
        }

        // 3. Aplica o recuo evitando sobreposição na barra de status e de gestos
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 4. Manipulador retrocompatível para o botão e gestos de voltar
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val tituloAtual = binding.titulo.text.toString()
                val descricaoAtual = binding.descricao.text.toString()
                val houveAlteracao = recuperarTitulo != tituloAtual || recuperarDescricao != descricaoAtual

                if (houveAlteracao && (tituloAtual.isNotEmpty() || descricaoAtual.isNotEmpty())) {
                    val tituloFinal = when {
                        tituloAtual.isEmpty() && descricaoAtual.length > MAX_TITULO_LENGTH -> descricaoAtual.substring(0, MAX_TITULO_LENGTH)
                        tituloAtual.isEmpty() -> descricaoAtual
                        else -> tituloAtual
                    }
                    val descricaoFinal = if (descricaoAtual.isEmpty()) tituloAtual else descricaoAtual

                    scope.launch {
                        if (id != null && id != 0L) {
                            atualizarNota(notaId, tituloFinal, descricaoFinal, hora)
                        } else {
                            criarNota(notaId, tituloFinal, descricaoFinal, hora)
                        }
                        withContext(Dispatchers.Main) {
                            startActivity(Intent(this@CriarNota, MainActivity::class.java))
                            finish()
                        }
                    }
                } else {
                    finish()
                }
            }
        })

        setSupportActionBar(binding.toolbar)
        bancoDeDados = AppDataBase.getInstance(this).NotaDao()

        titulo = binding.titulo.text.toString()
        descricao = binding.descricao.text.toString()

        id = intent?.getLongExtra("id", 0L)
        recuperarTitulo = intent?.getStringExtra("titulo")
        recuperarDescricao = intent?.getStringExtra("descricao")

        if (id != null && id != 0L && recuperarTitulo != null && recuperarDescricao != null) {
            notaId = id!!
            titulo = binding.titulo.setText(recuperarTitulo).toString()
            descricao = binding.descricao.setText(recuperarDescricao).toString()
            updateQuantidadeCaracteres(recuperarTitulo!!.length)
        }

        val greenColor = getColor(R.color.verde_claro)
        binding.titulo.highlightColor = greenColor
        binding.descricao.highlightColor = greenColor

        binding.titulo.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val tituloLength = s?.length ?: 0
                binding.contadorCaracteres.text = "$tituloLength/${MAX_TITULO_LENGTH}"

                if (tituloLength >= MAX_TITULO_LENGTH) {
                    binding.contadorCaracteres.setTextColor(Color.RED)
                    s?.delete(MAX_TITULO_LENGTH, tituloLength)
                    binding.titulo.setSelection(binding.titulo.length())
                } else {
                    binding.contadorCaracteres.setTextColor(Color.parseColor("#676767"))
                }
            }
        })
    }

    private fun updateQuantidadeCaracteres(length: Int) {
        binding.contadorCaracteres.text = "$length/${MAX_TITULO_LENGTH}"
        binding.contadorCaracteres.setTextColor(
            if (length >= MAX_TITULO_LENGTH) Color.RED else Color.parseColor("#676767")
        )
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
                compartilharNota()
            }
            R.id.menu_remover -> {
                scope.launch { deletar() }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private suspend fun criarNota(id: Long, titulo: String, descricao: String, data: Long) {
        val nota = Notas(id, titulo, descricao, data)
        bancoDeDados.salva(nota)
    }

    private suspend fun salvar() {
        titulo = binding.titulo.text.toString()
        descricao = binding.descricao.text.toString()

        if (titulo.isEmpty() && descricao.isEmpty()) {
            withContext(Dispatchers.Main) {
                Snackbar.make(binding.root, R.string.snackbar_criar_nota, Snackbar.LENGTH_SHORT)
                    .apply {
                        setTextColor(Color.WHITE)
                        setBackgroundTint(Color.RED)
                        show()
                    }
            }
        } else {
            val tituloFinal = when {
                titulo.isEmpty() && descricao.length > MAX_TITULO_LENGTH -> descricao.substring(0, MAX_TITULO_LENGTH)
                titulo.isEmpty() -> descricao
                else -> titulo
            }
            val descricaoFinal = if (descricao.isEmpty()) titulo else descricao

            if (id != null && id != 0L) {
                atualizarNota(notaId, tituloFinal, descricaoFinal, hora)
            } else {
                criarNota(notaId, tituloFinal, descricaoFinal, hora)
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@CriarNota,
                    getString(R.string.anotacao_salva_com_sucesso),
                    Toast.LENGTH_SHORT
                ).show()
                startActivity(Intent(this@CriarNota, MainActivity::class.java))
                finish()
            }
        }
    }

    private suspend fun deletar() {
        titulo = binding.titulo.text.toString()
        descricao = binding.descricao.text.toString()

        if (titulo.isEmpty() && descricao.isEmpty()) {
            finish()
        } else {
            withContext(Dispatchers.Main) {
                val dialogBinding = DialogExclusaoActivityCriarNotaBinding.inflate(layoutInflater)
                val exibirDialog = AlertDialog.Builder(this@CriarNota)
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
                        if (currentUser != null) {
                            val excluirFirebase = db.excluirAnotacoesUsuario(notaId)
                            if (excluirFirebase) {
                                bancoDeDados.remover(notaId)
                            }
                        } else {
                            bancoDeDados.remover(notaId)
                        }
                        withContext(Dispatchers.Main) {
                            finish()
                        }
                    }
                }
            }
        }
    }

    private fun compartilharNota() {
        titulo = binding.titulo.text.toString()
        descricao = binding.descricao.text.toString()

        val txtDados = "${titulo}\n${descricao}"
        val nomeArquivo = "$titulo.txt"
        val arquivo = File(filesDir, nomeArquivo)
        arquivo.writeText(txtDados)

        val uri = FileProvider.getUriForFile(
            this@CriarNota,
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

    suspend fun atualizarNota(id: Long, titulo: String, descricao: String, data: Long) {
        val nota = Notas(id, titulo, descricao, data)
        bancoDeDados.atualizar(nota)
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

    companion object {
        private const val MAX_TITULO_LENGTH = 30
    }
}