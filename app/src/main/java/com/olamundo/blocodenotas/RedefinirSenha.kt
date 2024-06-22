package com.olamundo.blocodenotas

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.olamundo.blocodenotas.databinding.ActivityRedefinirSenhaBinding
import java.util.Locale

class RedefinirSenha : AppCompatActivity() {
    private lateinit var binding: ActivityRedefinirSenhaBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        carregarLocalidade()
        binding = ActivityRedefinirSenhaBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        loadTheme()
        // Definindo a cor de seleção do texto para verde
        val greenColor = getColor(R.color.verde_claro) // Certifique-se de ter definido a cor verde no colors.xml
        binding.email.highlightColor = greenColor
        binding.senhaAtual.highlightColor = greenColor
        binding.novaSenha.highlightColor = greenColor
        binding.confirmarNovaSenha.highlightColor = greenColor


        binding.botaoRedefinirSenha.setOnClickListener {
            val email = binding.email.text.toString()
            val senhaAtual = binding.senhaAtual.text.toString()
            val confirmarSenha1 = binding.novaSenha.text.toString()
            val confirmarSenha2 = binding.confirmarNovaSenha.text.toString()

            recolherTeclado()

            if (email.isEmpty() || senhaAtual.isEmpty() || confirmarSenha1.isEmpty() || confirmarSenha2.isEmpty()) {
                Snackbar.make(
                    binding.root,
                    getString(R.string.preencha_todos_os_campos),
                    Snackbar.LENGTH_SHORT
                ).apply {
                    this.setBackgroundTint(Color.RED)
                    this.setTextColor(Color.WHITE)
                    this.show()
                }
            } else {
                if (confirmarSenha1 == confirmarSenha2) {
                    val usuario = FirebaseAuth.getInstance().currentUser

                    usuario?.let {

                        val credenciais = EmailAuthProvider.getCredential(email, senhaAtual)
                        usuario.reauthenticate(credenciais)

                            .addOnCompleteListener { reautenticacao ->
                                if (reautenticacao.isSuccessful) {
                                    usuario.updatePassword(confirmarSenha1)
                                        .addOnCompleteListener { tarefa ->
                                            if (tarefa.isSuccessful) {
                                                Snackbar.make(
                                                    binding.root,
                                                    getString(R.string.senha_alterada_com_sucesso),
                                                    Snackbar.LENGTH_SHORT
                                                ).apply {
                                                    this.setTextColor(Color.WHITE)
                                                    this.setBackgroundTint(Color.parseColor("#214C06"))
                                                    this.show()
                                                }
                                                Handler().postDelayed({
                                                    Intent(this, MainActivity::class.java).apply {
                                                        startActivity(this)
                                                    }
                                                }, 3000)
                                            } else {
                                                Snackbar.make(
                                                    binding.root,
                                                    getString(R.string.falha_ao_alterar_senha),
                                                    Snackbar.LENGTH_SHORT
                                                ).apply {
                                                    this.setTextColor(Color.WHITE)
                                                    this.setBackgroundTint(Color.RED)
                                                    this.show()
                                                }
                                            }
                                        }
                                } else {
                                    Snackbar.make(
                                        binding.root,
                                        getString(R.string.senha_ou_usuario_incorretos),
                                        Snackbar.LENGTH_SHORT
                                    ).apply {
                                        this.setBackgroundTint(Color.RED)
                                        this.setTextColor(Color.WHITE)
                                        this.show()
                                    }
                                }
                            }
                    } ?: run {
                        Snackbar.make(binding.root, getString(R.string.nao_foi_possivel_redefinir_a_senha_sem_usuario_logado), Snackbar.LENGTH_SHORT).apply {
                            this.setBackgroundTint(Color.RED)
                            this.setTextColor(Color.WHITE)
                            this.show()
                        }
                    }

                } else {
                    Snackbar.make(
                        binding.root,
                        getString(R.string.as_senhas_sao_divergentes),
                        Snackbar.LENGTH_SHORT
                    ).apply {
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
        binding.view.setBackgroundResource(R.drawable.container_backgrond_branco)
        binding.email.setBackgroundResource(R.drawable.shape_edit_text_preto)
        binding.senhaAtual.setBackgroundResource(R.drawable.shape_edit_text_preto)
        binding.novaSenha.setBackgroundResource(R.drawable.shape_edit_text_preto)
        binding.confirmarNovaSenha.setBackgroundResource(R.drawable.shape_edit_text_preto)
        binding.botaoRedefinirSenha.setBackgroundResource(R.drawable.shape_botao_preto)
    }

    private fun applyLightTheme() {
        binding.view.setBackgroundResource(R.drawable.container_background_preto)
        binding.email.setBackgroundResource(R.drawable.shape_edit_text_branco)
        binding.senhaAtual.setBackgroundResource(R.drawable.shape_edit_text_branco)
        binding.novaSenha.setBackgroundResource(R.drawable.shape_edit_text_branco)
        binding.confirmarNovaSenha.setBackgroundResource(R.drawable.shape_edit_text_branco)
        binding.botaoRedefinirSenha.setBackgroundResource(R.drawable.shape_botao_branco)
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

        val configuration = resources.configuration
        configuration.setLocale(localidade)
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }

    private fun carregarLocalidade() {
        val preferences = getSharedPreferences("config_linguagens", MODE_PRIVATE)
        val linguagem = preferences.getString("minha_linguagem", "")
        if (linguagem != null) {
            selecionarIdioma(linguagem)
        }
    }
}