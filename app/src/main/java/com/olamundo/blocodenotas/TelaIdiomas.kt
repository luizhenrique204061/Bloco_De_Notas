package com.olamundo.blocodenotas

import Adapter.CustomSpinnerAdapter
import Modelo.SpinnerItem
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.olamundo.blocodenotas.databinding.ActivityTelaIdiomasBinding
import java.util.Locale

class TelaIdiomas : AppCompatActivity() {

    private lateinit var binding: ActivityTelaIdiomasBinding
    private lateinit var spinnerIdiomas: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        carregarLocalidade()
        binding = ActivityTelaIdiomasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        spinnerIdiomas = binding.seletorIdiomas
        val items = listOf(
            SpinnerItem(0, getString(R.string.idiomas)), // Primeiro item como "Idiomas"
            SpinnerItem(R.drawable.ic_flag_br, getString(R.string.portugues)),
            SpinnerItem(R.drawable.ic_flag_us, getString(R.string.ingles)),
            SpinnerItem(R.drawable.ic_flag_es, getString(R.string.espanhol)),
            SpinnerItem(R.drawable.ic_flag_ru, getString(R.string.russo)),
            SpinnerItem(0, getString(R.string.padrao)) // Para o item "Padrão" sem imagem
        )
        val adapter = CustomSpinnerAdapter(this, items)
        spinnerIdiomas.adapter = adapter

        spinnerIdiomas.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> {
                        // Não faz nada quando "Idiomas" é selecionado
                    }
                    1 -> selecionarIdioma("pt")
                    2 -> selecionarIdioma("en")
                    3 -> selecionarIdioma("es")
                    4 -> selecionarIdioma("ru")
                    5 -> limparSharedPreferences()
                }
                if (position != 0) {
                    Intent(this@TelaIdiomas, MainActivity::class.java).apply {
                        startActivity(this)
                        finish()
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
    }

    private fun limparSharedPreferences() {
        val editor = getSharedPreferences("config_linguagens", MODE_PRIVATE).edit()
        editor.clear()
        editor.apply()
        Intent(this, MainActivity::class.java).apply {
            startActivity(this)
            finish()
        }
    }

    private fun selecionarIdioma(linguagem: String) {
        val localidade = Locale(linguagem)
        Locale.setDefault(localidade)

        val configuration = resources.configuration
        configuration.setLocale(localidade)
        resources.updateConfiguration(configuration, resources.displayMetrics)

        val editor = getSharedPreferences("config_linguagens", MODE_PRIVATE).edit()
        editor.putString("minha_linguagem", linguagem)
        editor.apply()
    }

    private fun carregarLocalidade() {
        val preferences = getSharedPreferences("config_linguagens", MODE_PRIVATE)
        val localidadeDoDispositivo = Locale.getDefault().language
        val linguagem = preferences.getString("minha_linguagem", localidadeDoDispositivo)
        if (linguagem != null) {
            selecionarIdioma(linguagem)
        }
    }
}