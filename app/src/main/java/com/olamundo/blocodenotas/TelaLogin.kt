package com.olamundo.blocodenotas

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.inputmethod.InputMethodManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.olamundo.blocodenotas.databinding.ActivityTelaLoginBinding
import java.util.Locale

class TelaLogin : AppCompatActivity() {
    private lateinit var binding: ActivityTelaLoginBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        carregarLocalidade()
        binding = ActivityTelaLoginBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        loadTheme()

        binding.botaoEntrar.setOnClickListener {
            val email = binding.email.text.toString()
            val senha = binding.senha.text.toString()

            recolherTeclado()

            if (email.isNotEmpty() && senha.isNotEmpty()) {

                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, senha)

                    .addOnCompleteListener { tarefa ->

                        if (tarefa.isSuccessful) {
                            Snackbar.make(
                                binding.root,
                                getString(R.string.login_com_email_e_senha),
                                Snackbar.LENGTH_SHORT
                            ).apply {
                                this.setBackgroundTint(Color.parseColor("#214C06"))
                                this.setTextColor(Color.WHITE)
                                this.show()
                            }
                            Handler().postDelayed({
                                Intent(this, TelaPrincipalProtegida::class.java).apply {
                                    startActivity(this)
                                    finish()
                                }
                            }, 3000)

                        } else {
                            Snackbar.make(
                                binding.root,
                                getString(R.string.falha_login_email_senha),
                                Snackbar.LENGTH_SHORT
                            ).apply {
                                this.setBackgroundTint(Color.RED)
                                this.setTextColor(Color.WHITE)
                                this.show()
                            }
                        }

                    }.addOnFailureListener { exception ->
                        when (exception) {
                            is FirebaseAuthInvalidUserException -> {
                                // Usuário não encontrado
                                Snackbar.make(
                                    binding.root,
                                    getString(R.string.usuario_nao_encontrado),
                                    Snackbar.LENGTH_SHORT
                                ).apply {
                                    this.setBackgroundTint(Color.RED)
                                    this.setTextColor(Color.WHITE)
                                    this.show()
                                }
                            }

                            is FirebaseAuthInvalidCredentialsException -> {
                                // Credenciais inválidas, ou seja, senha incorreta
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

                            else -> {
                                // Outra exceção
                                Snackbar.make(
                                    binding.root,
                                    getString(R.string.erro_desconhcido_login_email_e_senha),
                                    Snackbar.LENGTH_SHORT
                                ).apply {
                                    this.setBackgroundTint(Color.RED)
                                    this.setTextColor(Color.WHITE)
                                    this.show()
                                }
                            }
                        }
                    }
            } else {
                Snackbar.make(binding.root, getString(R.string.preencha_todos_os_campos), Snackbar.LENGTH_SHORT).apply {
                    this.setBackgroundTint(Color.RED)
                    this.setTextColor(Color.WHITE)
                    this.show()
                }
            }
        }

        binding.botaoCadastrar.setOnClickListener {
            Intent(this, TelaCadastroNotasProtegidas::class.java).apply {
                startActivity(this)
            }
        }

        binding.botaoEsqueciMinhaSenha.setOnClickListener {
            val email = binding.email.text.toString()

            if (email.isEmpty()) {
                recolherTeclado()
                Snackbar.make(binding.root, getString(R.string.digite_email_para_recuperar), Snackbar.LENGTH_SHORT).apply {
                    this.setBackgroundTint(Color.RED)
                    this.setTextColor(Color.WHITE)
                    this.show()
                }
            } else {
                recolherTeclado()
                FirebaseAuth.getInstance().fetchSignInMethodsForEmail(email)
                    .addOnCompleteListener { resultado ->
                        if (resultado.isSuccessful) {
                            FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                                .addOnCompleteListener { tarefa ->
                                    if (tarefa.isSuccessful) {
                                        Snackbar.make(binding.root, getString(R.string.email_de_recuperacao_enviado_com_sucesso), Snackbar.LENGTH_SHORT).apply {
                                            this.setTextColor(Color.WHITE)
                                            this.setBackgroundTint(Color.parseColor("#214C06"))
                                            this.show()
                                        }
                                    } else {
                                        Snackbar.make(binding.root, getString(R.string.erro_ao_enviar_email_de_recuperacao), Snackbar.LENGTH_SHORT).apply {
                                            this.setTextColor(Color.WHITE)
                                            this.setBackgroundTint(Color.RED)
                                            this.show()
                                        }
                                    }
                                }
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
        binding.senha.setBackgroundResource(R.drawable.shape_edit_text_preto)
        binding.botaoEntrar.setBackgroundResource(R.drawable.shape_botao_preto)
      //  binding.imagemCadeado.setBackgroundResource(R.drawable.ic_cadeado)
    }

    private fun applyLightTheme() {
        binding.view.setBackgroundResource(R.drawable.container_background_preto)
        binding.email.setBackgroundResource(R.drawable.shape_edit_text_branco)
        binding.senha.setBackgroundResource(R.drawable.shape_edit_text_branco)
        binding.botaoEntrar.setBackgroundResource(R.drawable.shape_botao_branco)
      //  binding.imagemCadeado.setBackgroundResource(R.drawable.ic_cadeado_preto)
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
        val linguagem = preferences.getString("minha_linguagem", "")
        if (linguagem != null) {
            selecionarIdioma(linguagem)
        }
    }
}