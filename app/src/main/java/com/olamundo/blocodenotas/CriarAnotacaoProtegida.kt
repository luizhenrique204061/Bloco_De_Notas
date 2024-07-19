package com.olamundo.blocodenotas

import DB.DB
import android.content.Intent
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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
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

        setSupportActionBar(binding.toolbar)

        anotacaoId = intent?.getStringExtra("anotacaoId")
        recuperarTitulo = intent?.getStringExtra("titulo")
        recuperarDescricao = intent?.getStringExtra("descricao")

        Log.i("ID", anotacaoId.toString())
        Log.i("tituloProtegido", recuperarTitulo.toString())
        Log.i("descricaoProtegido", recuperarDescricao.toString())

        if (anotacaoId != null && recuperarTitulo != null && recuperarDescricao != null) {
            id = anotacaoId!!


            titulo = binding.titulo.setText(recuperarTitulo).toString()
            descricao = binding.descricao.setText(recuperarDescricao).toString()
            updateQuantidadeCaracteres(recuperarTitulo!!.length)
        }

        // Definindo a cor de seleção do texto para verde
        val greenColor = getColor(R.color.verde_claro) // Certifique-se de ter definido a cor verde no colors.xml
        binding.titulo.highlightColor = greenColor
        binding.descricao.highlightColor = greenColor


        //Adicionando TextWatcher para monitorar o título
        binding.titulo.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Nenhuma ação necessária antes da mudança de texto
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Nenhuma ação necessária antes da mudança de texto
            }

            override fun afterTextChanged(s: Editable?) {
                // Atualizar o TextView com a contagem de caracteres
                val tituloLength = s?.length ?: 0
                binding.contadorCaracteres.text = "$tituloLength/${MAX_TITULO_LENGTH}"

                // Verificar se o limite foi atingido
                if (tituloLength >= MAX_TITULO_LENGTH) {
                    // Alterar a cor do contador para vermelho
                    binding.contadorCaracteres.setTextColor(Color.RED)

                    // Remover o último caractere excedente
                    s?.delete(MAX_TITULO_LENGTH, tituloLength)

                    // Mover o cursor para o final do texto
                    binding.titulo.setSelection(binding.titulo.length())

                    // Exibir um Toast informando que o limite foi atingido
                    // Toast.makeText(this@CriarNota, getString(R.string.limite_de_caracteres_do_titulo), Toast.LENGTH_SHORT).show()
                } else {
                    // Caso contrário, manter a cor padrão do contador
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
            R.id.menu_salvar -> {
                salvar()

            }

            R.id.menu_compartilhar -> {
                compartilharNota()
            }

            R.id.menu_remover -> {
                deletar(id)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun salvar() {
        titulo = binding.titulo.text.toString()
        descricao = binding.descricao.text.toString()
        if (titulo.isEmpty() && descricao.isEmpty()) {
            Snackbar.make(binding.root, R.string.snackbar_criar_nota, Snackbar.LENGTH_SHORT)
                .apply {
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

                Toast.makeText(
                    this@CriarAnotacaoProtegida,
                    getString(R.string.anotacao_salva_com_sucesso),
                    Toast.LENGTH_SHORT
                ).show()
                Intent(this@CriarAnotacaoProtegida, TelaPrincipalProtegida::class.java).apply {
                    startActivity(this)
                }

            } else {
                titulo = descricao
                if (anotacaoId != null) {
                    db.atualizarAnotacaoProtegida(anotacaoId!!, titulo, descricao, hora)
                } else {
                    db.salvarAnotacoesProtegidas(titulo, descricao, hora)

                }

                Toast.makeText(
                    this@CriarAnotacaoProtegida,
                    getString(R.string.anotacao_salva_com_sucesso),
                    Toast.LENGTH_SHORT
                ).show()
                Intent(this@CriarAnotacaoProtegida, TelaPrincipalProtegida::class.java).apply {
                    startActivity(this)
                }
            }

            finish()

        } else if (descricao.isEmpty()) {
            descricao = titulo
            if (anotacaoId != null) {
                db.atualizarAnotacaoProtegida(anotacaoId!!, titulo, descricao, hora)
            } else {
                db.salvarAnotacoesProtegidas(titulo, descricao, hora)

            }

            Toast.makeText(
                this@CriarAnotacaoProtegida,
                getString(R.string.anotacao_salva_com_sucesso),
                Toast.LENGTH_SHORT
            ).show()
            Intent(this@CriarAnotacaoProtegida, TelaPrincipalProtegida::class.java).apply {
                startActivity(this)
            }

            finish()

        } else {
            if (anotacaoId != null) {
                db.atualizarAnotacaoProtegida(anotacaoId!!, titulo, descricao, hora)
            } else {
                db.salvarAnotacoesProtegidas(titulo, descricao, hora)

            }

            Toast.makeText(
                this@CriarAnotacaoProtegida,
                getString(R.string.anotacao_salva_com_sucesso),
                Toast.LENGTH_SHORT
            ).show()
            Intent(this@CriarAnotacaoProtegida, TelaPrincipalProtegida::class.java).apply {
                startActivity(this)
            }
            finish()
        }
    }



    private fun updateQuantidadeCaracteres(length: Int) {
        binding.contadorCaracteres.text = "$length/${MAX_TITULO_LENGTH}"
        binding.contadorCaracteres.setTextColor(
            if (length >= MAX_TITULO_LENGTH) Color.RED else Color.parseColor(
                "#676767"
            )
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
            this@CriarAnotacaoProtegida,
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

        } else  {

            val dialogBinding = DialogExclusaoActivityCriarNotaBinding.inflate(layoutInflater)
            val exibirDialog = AlertDialog.Builder(this@CriarAnotacaoProtegida)
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
                Log.i("Excluir", idAnotacao)
                db.excluirAnotacoesProtegidas(idAnotacao)
                finish()
            }

        }

    }

    override fun onBackPressed() {
        titulo = binding.titulo.text.toString()
        descricao = binding.descricao.text.toString()
        if (titulo.isEmpty() && descricao.isEmpty()) {


        } else if (titulo.isEmpty()) {
            if (descricao.length > MAX_TITULO_LENGTH) {
                val tituloFormatado = descricao.substring(0, MAX_TITULO_LENGTH)
                titulo = tituloFormatado

                if (anotacaoId != null) {
                    db.atualizarAnotacaoProtegida(anotacaoId!!, titulo, descricao, hora)
                } else {
                    db.salvarAnotacoesProtegidas(titulo, descricao, hora)

                }

                Intent(this@CriarAnotacaoProtegida, TelaPrincipalProtegida::class.java).apply {
                    startActivity(this)
                }

            } else {
                titulo = descricao
                if (anotacaoId != null) {
                    db.atualizarAnotacaoProtegida(anotacaoId!!, titulo, descricao, hora)
                } else {
                    db.salvarAnotacoesProtegidas(titulo, descricao, hora)

                }

                Intent(this@CriarAnotacaoProtegida, TelaPrincipalProtegida::class.java).apply {
                    startActivity(this)
                }
            }

            finish()

        } else if (descricao.isEmpty()) {
            descricao = titulo
            if (anotacaoId != null) {
                db.atualizarAnotacaoProtegida(anotacaoId!!, titulo, descricao, hora)
            } else {
                db.salvarAnotacoesProtegidas(titulo, descricao, hora)

            }

            Intent(this@CriarAnotacaoProtegida, TelaPrincipalProtegida::class.java).apply {
                startActivity(this)
            }

            finish()

        } else {
            if (anotacaoId != null) {
                db.atualizarAnotacaoProtegida(anotacaoId!!, titulo, descricao, hora)
            } else {
                db.salvarAnotacoesProtegidas(titulo, descricao, hora)

            }

            Intent(this@CriarAnotacaoProtegida, TelaPrincipalProtegida::class.java).apply {
                startActivity(this)
            }
            finish()
        }
        super.onBackPressed()
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

    private fun carregarAnuncioBanner() {
        //Anúncio do Tipo Banner

        MobileAds.initialize(this)
        mAdview = binding.adview
        val adRequest = AdRequest.Builder().build()
        Log.i("Meu App", "Antes de carregar o anúncio")
        mAdview.loadAd(adRequest)
    }

    companion object {
        private const val MAX_TITULO_LENGTH = 30
    }
}