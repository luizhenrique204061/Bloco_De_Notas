package com.olamundo.blocodenotas

import Modelo.Notas
import Room.AppDataBase
import Room.NotaDao
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.olamundo.blocodenotas.databinding.ActivityCriarNotaBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        when(item.itemId) {
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
        val titulo = binding.titulo.text.toString()
        val descricao = binding.descricao.text.toString()
        criarNota(notaId, titulo, descricao, hora)

        Log.d("CriarNota", "Nota salva com ID: $notaId - Título: $titulo, Descrição: $descricao")
    }
}