package com.olamundo.blocodenotas

import DB.DB
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.olamundo.blocodenotas.databinding.ActivityAlterarNomeUsuarioBinding
import java.util.Locale

class AlterarNomeUsuario : AppCompatActivity() {
    private lateinit var binding: ActivityAlterarNomeUsuarioBinding
    val db = DB()
    override fun onCreate(savedInstanceState: Bundle?) {
        carregarLocalidade()
        binding = ActivityAlterarNomeUsuarioBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        loadTheme()
        // Definindo a cor de seleção do texto para verde
        val greenColor = getColor(R.color.verde_claro) // Certifique-se de ter definido a cor verde no colors.xml
        binding.novoNome.highlightColor = greenColor
        binding.confirmeONovoNome.highlightColor = greenColor

        val nomeUsuario = binding.mostrarUsuario

        db.mostrarUsuarioActivityAlterarNome(nomeUsuario)

        binding.botaoConfirmarAlteracao.setOnClickListener {
            recolherTeclado()

            val primeiroNome = binding.novoNome.text.toString()
            val segundoNome = binding.confirmeONovoNome.text.toString()

            if (primeiroNome.isEmpty() && segundoNome.isEmpty()) {
                Snackbar.make(binding.root, getString(R.string.preencha_todos_os_campos), Snackbar.LENGTH_SHORT).apply {
                    this.setBackgroundTint(Color.RED)
                    this.setTextColor(Color.WHITE)
                    this.show()
                }
            } else {
                if (primeiroNome == segundoNome) {
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    currentUser?.let {
                        db.atualizarNomeUsuario(primeiroNome)
                        Snackbar.make(binding.root, getString(R.string.nome_do_usuario_alterado_com_secesso), Snackbar.LENGTH_SHORT).apply {
                            this.setBackgroundTint(Color.parseColor("#214C06"))
                            this.setTextColor(Color.WHITE)
                            this.show()
                        }


                    } ?: run {
                        Snackbar.make(binding.root, getString(R.string.falha_alteracao_nome_usuario), Snackbar.LENGTH_SHORT).apply {
                            this.setBackgroundTint(Color.RED)
                            this.setTextColor(Color.WHITE)
                            this.show()
                        }
                    }

                } else {
                    Snackbar.make(binding.root, getString(R.string.falha_os_nomes_de_usuario_sao_diferentes), Snackbar.LENGTH_SHORT).apply {
                        this.setBackgroundTint(Color.RED)
                        this.setTextColor(Color.WHITE)
                        this.show()
                    }
                }
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
        binding.novoNome.setBackgroundResource(R.drawable.shape_edit_text_branco)
        binding.confirmeONovoNome.setBackgroundResource(R.drawable.shape_edit_text_branco)
        binding.novoNome.setHintTextColor(Color.BLACK)
        binding.confirmeONovoNome.setHintTextColor(Color.BLACK)
        binding.novoNome.setTextColor(Color.BLACK)
        binding.confirmeONovoNome.setTextColor(Color.BLACK)
    }

    private fun applyLightTheme() {
        binding.novoNome.setBackgroundResource(R.drawable.shape_edit_text_preto)
        binding.confirmeONovoNome.setBackgroundResource(R.drawable.shape_edit_text_preto)
        binding.novoNome.setHintTextColor(Color.WHITE)
        binding.confirmeONovoNome.setHintTextColor(Color.WHITE)
        binding.novoNome.setTextColor(Color.WHITE)
        binding.confirmeONovoNome.setTextColor(Color.WHITE)
    }

    private fun recolherTeclado() {
        val inputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { viewTecaldo ->
            inputMethodManager.hideSoftInputFromWindow(viewTecaldo.windowToken, 0)
        }
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
}