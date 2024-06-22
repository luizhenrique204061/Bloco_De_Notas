package com.olamundo.blocodenotas

import DB.DB
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.olamundo.blocodenotas.databinding.ActivityTelaCadastroNotasProtegidasBinding
import java.util.Locale

class TelaCadastroNotasProtegidas : AppCompatActivity() {
    private lateinit var binding: ActivityTelaCadastroNotasProtegidasBinding
    val db = DB()
    private lateinit var progressBar: ProgressBar
    override fun onCreate(savedInstanceState: Bundle?) {
        carregarLocalidade()
        binding = ActivityTelaCadastroNotasProtegidasBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        progressBar = binding.progressbar

        loadTheme()

        // Definindo a cor de seleção do texto para verde
        val greenColor = getColor(R.color.verde_claro) // Certifique-se de ter definido a cor verde no colors.xml
        binding.nomeUsuario.highlightColor = greenColor
        binding.email.highlightColor = greenColor
        binding.senha.highlightColor = greenColor

        binding.botaoCadastrar.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            binding.botaoCadastrar.setText("")
            val nomeUsuario = binding.nomeUsuario.text.toString()
            val editEmail = binding.email.text.toString()
            val editSenha = binding.senha.text.toString()

            recolherTeclado()

            if (nomeUsuario.isNotEmpty() && editEmail.isNotEmpty() && editSenha.isNotEmpty()) {
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(editEmail, editSenha)
                    .addOnCompleteListener { tarefa ->

                        if (tarefa.isSuccessful) {
                            db.salvarNomeUsuario(nomeUsuario)
                            Snackbar.make(
                                binding.root,
                                getString(R.string.cadastro_email_e_senha_com_sucesso),
                                Snackbar.LENGTH_SHORT
                            ).apply {
                                this.setBackgroundTint(Color.parseColor("#214C06"))
                                this.setTextColor(Color.WHITE)
                                this.show()
                            }
                            Handler().postDelayed({
                                progressBar.visibility = View.GONE
                                binding.botaoCadastrar.setText(getString(R.string.cadastrar))
                                Intent(this, TelaLogin::class.java).apply {
                                    startActivity(this)
                                    finish()
                                }
                            }, 3000)

                        } else {
                            Snackbar.make(
                                binding.root,
                                getString(R.string.falha_cadastro_email_e_senha),
                                Snackbar.LENGTH_SHORT
                            ).apply {
                                this.setBackgroundTint(Color.RED)
                                this.setTextColor(Color.WHITE)
                                this.show()
                            }
                            progressBar.visibility = View.GONE
                            binding.botaoCadastrar.setText(getString(R.string.cadastrar))
                        }

                    }.addOnFailureListener { exception ->
                        when (exception) {
                            is FirebaseAuthUserCollisionException -> {
                                Snackbar.make(
                                    binding.root,
                                    getString(R.string.falha_conta_cadastrada),
                                    Snackbar.LENGTH_SHORT
                                ).apply {
                                    this.setBackgroundTint(Color.RED)
                                    this.setTextColor(Color.WHITE)
                                    this.show()
                                    progressBar.visibility = View.GONE
                                    binding.botaoCadastrar.setText(getString(R.string.cadastrar))
                                }
                            }

                            is FirebaseAuthWeakPasswordException -> {
                                Snackbar.make(
                                    binding.root,
                                    getString(R.string.falha_caracteres),
                                    Snackbar.LENGTH_SHORT
                                ).apply {
                                    this.setBackgroundTint(Color.RED)
                                    this.setTextColor(Color.WHITE)
                                    this.show()
                                }
                                progressBar.visibility = View.GONE
                                binding.botaoCadastrar.setText(getString(R.string.cadastrar))
                            }

                            else -> {
                                Snackbar.make(
                                    binding.root,
                                    getString(R.string.erro_desconhcido_login_email_e_senha),
                                    Snackbar.LENGTH_SHORT
                                ).apply {
                                    this.setBackgroundTint(Color.RED)
                                    this.setTextColor(Color.WHITE)
                                    this.show()
                                }
                                progressBar.visibility = View.GONE
                                binding.botaoCadastrar.setText(getString(R.string.cadastrar))
                            }
                        }
                    }
            } else {
                Snackbar.make(binding.root, getString(R.string.preencha_todos_os_campos), Snackbar.LENGTH_SHORT).apply {
                    this.setBackgroundTint(Color.RED)
                    this.setTextColor(Color.WHITE)
                    this.show()
                }
                progressBar.visibility = View.GONE
                binding.botaoCadastrar.setText(getString(R.string.cadastrar))
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
        binding.botaoCadastrar.setBackgroundResource(R.drawable.shape_botao_preto)
        binding.nomeUsuario.setBackgroundResource(R.drawable.shape_edit_text_preto)
    }

    private fun applyLightTheme() {
        binding.view.setBackgroundResource(R.drawable.container_background_preto)
        binding.email.setBackgroundResource(R.drawable.shape_edit_text_branco)
        binding.senha.setBackgroundResource(R.drawable.shape_edit_text_branco)
        binding.botaoCadastrar.setBackgroundResource(R.drawable.shape_botao_branco)
        binding.nomeUsuario.setBackgroundResource(R.drawable.shape_edit_text_branco)
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