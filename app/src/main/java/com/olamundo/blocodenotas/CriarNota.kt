package com.olamundo.blocodenotas

import DB.DB
import Modelo.Notas
import Room.AppDataBase
import Room.NotaDao
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.olamundo.blocodenotas.databinding.ActivityCriarNotaBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
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
    override fun onCreate(savedInstanceState: Bundle?) {
        carregarLocalidade()
        binding = ActivityCriarNotaBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        bancoDeDados = AppDataBase.getInstance(this).NotaDao()

        val id = intent.getLongExtra("id", 0L)
        val recuperarTitulo = intent.getStringExtra("titulo")
        val recuperarDescricao = intent.getStringExtra("descricao")

        if (id != null && recuperarTitulo != null && recuperarDescricao != null) {
            notaId = id
            titulo = binding.titulo.setText(recuperarTitulo).toString()
            descricao = binding.descricao.setText(recuperarDescricao).toString()
            updateQuantidadeCaracteres(recuperarTitulo.length)
        }


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

    private fun updateQuantidadeCaracteres(length: Int) {
        binding.contadorCaracteres.text = "$length/${MAX_TITULO_LENGTH}"
        binding.contadorCaracteres.setTextColor(
            if (length >= MAX_TITULO_LENGTH) Color.RED else Color.parseColor(
                "#676767"
            )
        )
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
                compartilharNota()
            }

            R.id.menu_remover -> {
                scope.launch {
                    deletar()
                }
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
                        this.setTextColor(Color.WHITE)
                        this.setBackgroundTint(Color.RED)
                        this.show()
                    }
            }
        } else if (titulo.isEmpty()) {
            if (descricao.length > MAX_TITULO_LENGTH) {
                val tituloFormatado = descricao.substring(0, MAX_TITULO_LENGTH)
                titulo = tituloFormatado

                criarNota(notaId, titulo, descricao, hora)
                Log.d(
                    "CriarNota",
                    "Nota salva com ID: $notaId - Título: $titulo, Descrição: $descricao"
                )
                scope.launch {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@CriarNota,
                            getString(R.string.anotacao_salva_com_sucesso),
                            Toast.LENGTH_SHORT
                        ).show()
                        Intent(this@CriarNota, MainActivity::class.java).apply {
                            startActivity(this)
                        }
                    }
                }
            } else {
                titulo = descricao
                criarNota(notaId, titulo, descricao, hora)
                Log.d(
                    "CriarNota",
                    "Nota salva com ID: $notaId - Título: $titulo, Descrição: $descricao"
                )
                scope.launch {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@CriarNota,
                            getString(R.string.anotacao_salva_com_sucesso),
                            Toast.LENGTH_SHORT
                        ).show()
                        Intent(this@CriarNota, MainActivity::class.java).apply {
                            startActivity(this)
                        }
                    }
                }
            }

            finish()

        } else if (descricao.isEmpty()) {
            descricao = titulo
            criarNota(notaId, titulo, descricao, hora)
            Log.d(
                "CriarNota",
                "Nota salva com ID: $notaId - Título: $titulo, Descrição: $descricao"
            )
            scope.launch {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@CriarNota,
                        getString(R.string.anotacao_salva_com_sucesso),
                        Toast.LENGTH_SHORT
                    ).show()
                    Intent(this@CriarNota, MainActivity::class.java).apply {
                        startActivity(this)
                    }
                }
            }
            finish()

        } else {
            criarNota(notaId, titulo, descricao, hora)
            Log.d(
                "CriarNota",
                "Nota salva com ID: $notaId - Título: $titulo, Descrição: $descricao"
            )
            scope.launch {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@CriarNota,
                        getString(R.string.anotacao_salva_com_sucesso),
                        Toast.LENGTH_SHORT
                    ).show()
                    Intent(this@CriarNota, MainActivity::class.java).apply {
                        startActivity(this)
                    }
                }
            }
            finish()
        }
    }

    private suspend fun deletar() {
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            val excluirFirebase = db.excluirAnotacoesUsuario(notaId)
            if (excluirFirebase) {
                // Se excluiu com sucesso do Firebase, exclui do Room
                bancoDeDados.remover(notaId)
            }

        } else {
            bancoDeDados.remover(notaId)
        }
        finish()
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


    override fun onBackPressed() {
        titulo = binding.titulo.text.toString()
        descricao = binding.descricao.text.toString()

        when {
            titulo.isEmpty() && descricao.isEmpty() -> {
                scope.launch {
                    withContext(Dispatchers.Main) {
                        //  Toast.makeText(this@CriarNota, getString(R.string.nenhum_texto_digitado), Toast.LENGTH_SHORT).show()
                        Intent(this@CriarNota, MainActivity::class.java).apply {
                            startActivity(this)
                        }
                    }
                }

            }

            titulo.isEmpty() -> {
                if (descricao.length > MAX_TITULO_LENGTH) {
                    val tituloFormatado = descricao.substring(0, MAX_TITULO_LENGTH)
                    titulo = tituloFormatado

                    scope.launch {
                        criarNota(notaId, titulo, descricao, hora)
                        Log.d(
                            "CriarNota",
                            "Nota salva com ID: $notaId - Título: $titulo, Descrição: $descricao"
                        )

                        withContext(Dispatchers.Main) {
                            //   Toast.makeText(this@CriarNota, getString(R.string.anotacao_salva_com_sucesso), Toast.LENGTH_SHORT).show()
                            Intent(this@CriarNota, MainActivity::class.java).apply {
                                startActivity(this)
                            }
                        }
                    }
                } else {
                    titulo = descricao
                    scope.launch {
                        criarNota(notaId, titulo, descricao, hora)
                        Log.d(
                            "CriarNota",
                            "Nota salva com ID: $notaId - Título: $titulo, Descrição: $descricao"
                        )

                        withContext(Dispatchers.Main) {
                            //   Toast.makeText(this@CriarNota, getString(R.string.anotacao_salva_com_sucesso), Toast.LENGTH_SHORT).show()
                            Intent(this@CriarNota, MainActivity::class.java).apply {
                                startActivity(this)
                            }
                        }
                    }
                }

            }

            descricao.isEmpty() -> {
                descricao = titulo
                scope.launch {
                    criarNota(notaId, titulo, descricao, hora)
                    Log.d(
                        "CriarNota",
                        "Nota salva com ID: $notaId - Título: $titulo, Descrição: $descricao"
                    )

                    withContext(Dispatchers.Main) {
                        //  Toast.makeText(this@CriarNota, getString(R.string.anotacao_salva_com_sucesso), Toast.LENGTH_SHORT).show()
                        Intent(this@CriarNota, MainActivity::class.java).apply {
                            startActivity(this)
                        }
                    }
                }
            }

            else -> {
                scope.launch {
                    criarNota(notaId, titulo, descricao, hora)
                    withContext(Dispatchers.Main) {
                        //   Toast.makeText(this@CriarNota, getString(R.string.anotacao_salva_com_sucesso), Toast.LENGTH_SHORT).show()
                        Intent(this@CriarNota, MainActivity::class.java).apply {
                            startActivity(this)
                        }
                    }
                }
            }
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
        val linguagem = preferences.getString("minha_linguagem", "")
        if (linguagem != null) {
            selecionarIdioma(linguagem)
        }
    }

    companion object {
        private const val MAX_TITULO_LENGTH = 30
    }
}