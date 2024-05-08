package com.olamundo.blocodenotas

import Modelo.Notas
import Room.AppDataBase
import Room.NotaDao
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.olamundo.blocodenotas.databinding.ActivityCriarNotaBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CriarNota : AppCompatActivity() {
    val notaId: Long = 0
    val hora = System.currentTimeMillis()
    private lateinit var bancoDeDados: NotaDao
    val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var binding: ActivityCriarNotaBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityCriarNotaBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        bancoDeDados = AppDataBase.getInstance(this).NotaDao()
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

            }

            R.id.menu_remover -> {

            }
        }
        return super.onOptionsItemSelected(item)
    }

    private suspend fun criarNota(id: Long, titulo: String, descricao: String, data: Long) {
        val nota = Notas(id, titulo, descricao, data)
        bancoDeDados.salva(nota)
    }

    private suspend fun salvar() {
        var titulo = binding.titulo.text.toString()
        var descricao = binding.descricao.text.toString()

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
            titulo = descricao
            criarNota(notaId, titulo, descricao, hora)
            Log.d(
                "CriarNota",
                "Nota salva com ID: $notaId - Título: $titulo, Descrição: $descricao"
            )
            scope.launch {
                withContext(Dispatchers.Main) {
                    Intent(this@CriarNota, MainActivity::class.java).apply {
                        startActivity(this)
                    }
                }
            }
        } else if (descricao.isEmpty()) {
            descricao = titulo
            criarNota(notaId, titulo, descricao, hora)
            Log.d(
                "CriarNota",
                "Nota salva com ID: $notaId - Título: $titulo, Descrição: $descricao"
            )
            scope.launch {
                withContext(Dispatchers.Main) {
                    Intent(this@CriarNota, MainActivity::class.java).apply {
                        startActivity(this)
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        var titulo = binding.titulo.text.toString()
        var descricao = binding.descricao.text.toString()

        when {
            titulo.isEmpty() && descricao.isEmpty() -> {
                scope.launch {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CriarNota, getString(R.string.nenhum_texto_digitado), Toast.LENGTH_SHORT).show()
                        Intent(this@CriarNota, MainActivity::class.java).apply {
                            startActivity(this)
                        }
                    }
                }

            }
            titulo.isEmpty() -> {
                titulo = descricao
                scope.launch {
                    criarNota(notaId, titulo, descricao, hora)
                    Log.d("CriarNota", "Nota salva com ID: $notaId - Título: $titulo, Descrição: $descricao")

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CriarNota, getString(R.string.anotacao_salva_com_sucesso), Toast.LENGTH_SHORT).show()
                        Intent(this@CriarNota, MainActivity::class.java).apply {
                            startActivity(this)
                        }
                    }
                }

            }
            descricao.isEmpty() -> {
                descricao = titulo
                scope.launch {
                    criarNota(notaId, titulo, descricao, hora)
                    Log.d("CriarNota", "Nota salva com ID: $notaId - Título: $titulo, Descrição: $descricao")

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CriarNota, getString(R.string.anotacao_salva_com_sucesso), Toast.LENGTH_SHORT).show()
                        Intent(this@CriarNota, MainActivity::class.java).apply {
                            startActivity(this)
                        }
                    }
                }
            }
            else -> {
                scope.launch {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CriarNota, getString(R.string.nenhum_texto_digitado), Toast.LENGTH_SHORT).show()
                        Intent(this@CriarNota, MainActivity::class.java).apply {
                            startActivity(this)
                        }
                    }
                }
            }
        }
        super.onBackPressed()
    }
}