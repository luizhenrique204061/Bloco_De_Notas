package com.olamundo.blocodenotas

import Modelo.Notas
import Room.AppDataBase
import Room.NotaDao
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import com.olamundo.blocodenotas.databinding.ActivityAbrirNotaBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class AbrirNota : AppCompatActivity() {
    private lateinit var binding: ActivityAbrirNotaBinding
    var notaId: Long = 0
    val hora = System.currentTimeMillis()
    private lateinit var bancoDeDados: NotaDao
    val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var titulo: String
    private lateinit var descricao: String

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityAbrirNotaBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val isModoEscuro = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        // 1. Define a cor de fundo da barra de notificações
        window.statusBarColor = if (isModoEscuro) {
            ContextCompat.getColor(this, R.color.black)
        } else {
            ContextCompat.getColor(this, R.color.white)
        }

        // 2. Controla o contraste dos ícones (escuros no tema claro, brancos no tema escuro)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isModoEscuro
            isAppearanceLightNavigationBars = !isModoEscuro
        }

        // 3. Aplica o recuo evitando sobreposição na barra de notificações e botões de gestos
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 4. Manipulador retrocompatível do botão/gesto de voltar
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val tituloAtual = binding.titulo.text.toString()
                val descricaoAtual = binding.descricao.text.toString()

                if (tituloAtual.isNotEmpty() || descricaoAtual.isNotEmpty()) {
                    scope.launch {
                        criarNota(notaId, tituloAtual, descricaoAtual, hora)
                        withContext(Dispatchers.Main) {
                            startActivity(Intent(this@AbrirNota, MainActivity::class.java))
                            finish()
                        }
                    }
                } else {
                    startActivity(Intent(this@AbrirNota, MainActivity::class.java))
                    finish()
                }
            }
        })

        setSupportActionBar(binding.toolbar)
        bancoDeDados = AppDataBase.getInstance(this).NotaDao()
        abrirNotas()

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
            criarNota(notaId, titulo, descricao, hora)
            Log.d("CriarNota", "Nota salva com ID: $notaId - Título: $titulo, Descrição: $descricao")

            scope.launch {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AbrirNota, getString(R.string.anotacao_salva_com_sucesso), Toast.LENGTH_SHORT).show()
                    Intent(this@AbrirNota, MainActivity::class.java).apply {
                        startActivity(this)
                    }
                    finish()
                }
            }
        } else if (descricao.isEmpty()) {
            criarNota(notaId, titulo, descricao, hora)
            Log.d("CriarNota", "Nota salva com ID: $notaId - Título: $titulo, Descrição: $descricao")

            scope.launch {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AbrirNota, getString(R.string.anotacao_salva_com_sucesso), Toast.LENGTH_SHORT).show()
                    Intent(this@AbrirNota, MainActivity::class.java).apply {
                        startActivity(this)
                    }
                    finish()
                }
            }
        } else {
            criarNota(notaId, titulo, descricao, hora)
            Log.d("CriarNota", "Nota salva com ID: $notaId - Título: $titulo, Descrição: $descricao")
            scope.launch {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AbrirNota, getString(R.string.anotacao_salva_com_sucesso), Toast.LENGTH_SHORT).show()
                    Intent(this@AbrirNota, MainActivity::class.java).apply {
                        startActivity(this)
                    }
                    finish()
                }
            }
        }
    }

    private suspend fun deletar() {
        bancoDeDados.remover(notaId)
        finish()
    }

    private fun abrirNotas() {
        val uri: Uri? = intent.data

        if (uri != null) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val content = reader.readText()
                inputStream?.close()
                reader.close()

                binding.descricao.setText(content)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, getString(R.string.erro_abrir_arquivo), Toast.LENGTH_SHORT).show()
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
            this@AbrirNota,
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

    companion object {
        private const val MAX_TITULO_LENGTH = 30
    }
}