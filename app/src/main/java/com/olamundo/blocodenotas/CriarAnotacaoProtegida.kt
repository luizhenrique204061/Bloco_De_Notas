package com.olamundo.blocodenotas

import DB.DB
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
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
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
import com.olamundo.blocodenotas.databinding.ActivityCriarAnotacaoProtegidaBinding
import com.olamundo.blocodenotas.databinding.DialogExclusaoActivityCriarNotaBinding
import java.io.File
import java.util.Locale

class CriarAnotacaoProtegida : AppCompatActivity() {
    private lateinit var binding: ActivityCriarAnotacaoProtegidaBinding
    private lateinit var titulo: String
    val db = DB()
    private lateinit var descricao: String
    var id = ""
    val hora = System.currentTimeMillis()
    private var anotacaoId: String? = null
    private var recuperarTitulo: String? = null
    private var recuperarDescricao: String? = null
    lateinit var mAdview: AdView

    override fun onCreate(savedInstanceState: Bundle?) {
        carregarLocalidade()
        binding = ActivityCriarAnotacaoProtegidaBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val isModoEscuro = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        // 1. Sincroniza a cor de fundo da barra de status com o tema
        window.statusBarColor = if (isModoEscuro) {
            ContextCompat.getColor(this, R.color.black)
        } else {
            ContextCompat.getColor(this, R.color.white)
        }

        // 2. Controla o contraste dos ícones do sistema (bateria, horas, Wi-Fi)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isModoEscuro
            isAppearanceLightNavigationBars = !isModoEscuro
        }

        // 3. Aplica o recuo evitando sobreposição na barra superior e inferior
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 4. Manipulador retrocompatível para botão e gestos de voltar
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val tituloAtual = binding.titulo.text.toString()
                val descricaoAtual = binding.descricao.text.toString()

                if (recuperarTitulo != tituloAtual || recuperarDescricao != descricaoAtual) {
                    if (tituloAtual.isNotEmpty() || descricaoAtual.isNotEmpty()) {
                        val tituloFinal = when {
                            tituloAtual.isEmpty() && descricaoAtual.length > MAX_TITULO_LENGTH -> descricaoAtual.substring(0, MAX_TITULO_LENGTH)
                            tituloAtual.isEmpty() -> descricaoAtual
                            else -> tituloAtual
                        }
                        val descricaoFinal = if (descricaoAtual.isEmpty()) tituloAtual else descricaoAtual

                        if (anotacaoId != null) {
                            db.atualizarAnotacaoProtegida(anotacaoId!!, tituloFinal, descricaoFinal, hora)
                        } else {
                            db.salvarAnotacoesProtegidas(tituloFinal, descricaoFinal, hora)
                        }

                        Intent(this@CriarAnotacaoProtegida, TelaPrincipalProtegida::class.java).apply {
                            startActivity(this)
                        }
                    }
                    finish()
                } else {
                    finish()
                }
            }
        })

        setSupportActionBar(binding.toolbar)

        anotacaoId = intent?.getStringExtra("anotacaoId")
        recuperarTitulo = intent?.getStringExtra("titulo")
        recuperarDescricao = intent?.getStringExtra("descricao")

        if (anotacaoId != null && recuperarTitulo != null && recuperarDescricao != null) {
            id = anotacaoId!!
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_salvar_compartilhar_remover, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_salvar -> salvar()
            R.id.menu_compartilhar -> compartilharNota()
            R.id.menu_remover -> deletar(id)
        }
        return super.onOptionsItemSelected(item)
    }

    fun salvar() {
        titulo = binding.titulo.text.toString()
        descricao = binding.descricao.text.toString()
        if (titulo.isEmpty() && descricao.isEmpty()) {
            Snackbar.make(binding.root, R.string.snackbar_criar_nota, Snackbar.LENGTH_SHORT).apply {
                this.setTextColor(Color.WHITE)
                this.setBackgroundTint(Color.RED)
                this.show()
            }
        } else if (titulo.isEmpty()) {
            if (descricao.length > MAX_TITULO_LENGTH) {
                val tituloFormatado = descricao.substring(0, MAX_TITULO_LENGTH)
                titulo = tituloFormatado

                if (anotacaoId != null) {
                    db.atualizarAnotacaoProtegida(anotacaoId!!, titulo, descricao, hora)
                } else {
                    db.salvarAnotacoesProtegidas(titulo, descricao, hora)
                }
            } else {
                titulo = descricao
                if (anotacaoId != null) {
                    db.atualizarAnotacaoProtegida(anotacaoId!!, titulo, descricao, hora)
                } else {
                    db.salvarAnotacoesProtegidas(titulo, descricao, hora)
                }
            }

            Toast.makeText(this, getString(R.string.anotacao_salva_com_sucesso), Toast.LENGTH_SHORT).show()
            Intent(this, TelaPrincipalProtegida::class.java).apply { startActivity(this) }
            finish()
        } else if (descricao.isEmpty()) {
            descricao = titulo
            if (anotacaoId != null) {
                db.atualizarAnotacaoProtegida(anotacaoId!!, titulo, descricao, hora)
            } else {
                db.salvarAnotacoesProtegidas(titulo, descricao, hora)
            }

            Toast.makeText(this, getString(R.string.anotacao_salva_com_sucesso), Toast.LENGTH_SHORT).show()
            Intent(this, TelaPrincipalProtegida::class.java).apply { startActivity(this) }
            finish()
        } else {
            if (anotacaoId != null) {
                db.atualizarAnotacaoProtegida(anotacaoId!!, titulo, descricao, hora)
            } else {
                db.salvarAnotacoesProtegidas(titulo, descricao, hora)
            }

            Toast.makeText(this, getString(R.string.anotacao_salva_com_sucesso), Toast.LENGTH_SHORT).show()
            Intent(this, TelaPrincipalProtegida::class.java).apply { startActivity(this) }
            finish()
        }
    }

    private fun updateQuantidadeCaracteres(length: Int) {
        binding.contadorCaracteres.text = "$length/${MAX_TITULO_LENGTH}"
        binding.contadorCaracteres.setTextColor(
            if (length >= MAX_TITULO_LENGTH) Color.RED else Color.parseColor("#676767")
        )
    }

    private fun compartilharNota() {
        titulo = binding.titulo.text.toString()
        descricao = binding.descricao.text.toString()

        val txtDados = "${titulo}\n${descricao}"
        val nomeArquivo = "$titulo.txt"
        val arquivo = File(filesDir, nomeArquivo)
        arquivo.writeText(txtDados)

        val uri = FileProvider.getUriForFile(
            this,
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

    fun deletar(idAnotacao: String) {
        titulo = binding.titulo.text.toString()
        descricao = binding.descricao.text.toString()

        if (titulo.isEmpty() && descricao.isEmpty()) {
            finish()
        } else {
            val dialogBinding = DialogExclusaoActivityCriarNotaBinding.inflate(layoutInflater)
            val exibirDialog = AlertDialog.Builder(this)
                .setView(dialogBinding.root)
                .setCancelable(false)
                .create()

            exibirDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            exibirDialog.show()

            dialogBinding.botaoCancelar.setOnClickListener {
                exibirDialog.dismiss()
            }

            dialogBinding.botaoProsseguir.setOnClickListener {
                db.excluirAnotacoesProtegidas(idAnotacao)
                finish()
            }
        }
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