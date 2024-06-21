package com.olamundo.blocodenotas

import DB.DB
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.olamundo.blocodenotas.databinding.ActivityExcluirContaBinding
import com.olamundo.blocodenotas.databinding.DialogExclusaoDeContaBinding
import java.util.Locale

class ExcluirConta : AppCompatActivity() {
    private lateinit var binding: ActivityExcluirContaBinding
    val db = DB()
    override fun onCreate(savedInstanceState: Bundle?) {
        carregarLocalidade()
        binding = ActivityExcluirContaBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        loadTheme()

        binding.botaoAutenticar.setOnClickListener {
            recolherTeclado()

            val email = binding.email.text.toString()
            val senha = binding.senha.text.toString()

            if (email.isEmpty() && senha.isEmpty()) {
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
                val usuario = FirebaseAuth.getInstance().currentUser

                usuario?.let {
                    val credenciais = EmailAuthProvider.getCredential(email, senha)

                    usuario.reauthenticate(credenciais)

                        .addOnCompleteListener { tarefa ->
                            if (tarefa.isSuccessful) {
                                val dialog = DialogExclusaoDeContaBinding.inflate(layoutInflater)
                                val exibirDialog = AlertDialog.Builder(this)
                                    .setView(dialog.root)
                                    .show()

                                dialog.botaoCancelarExclusaoDeConta.setOnClickListener {
                                    exibirDialog.dismiss()
                                }

                                dialog.botaoProsseguirExclusaoDeConta.setOnClickListener {
                                    db.realizarExclusao(this)
                                    Intent(this, MainActivity::class.java).apply {
                                        startActivity(this)
                                        finish()
                                    }

                                }
                            } else {
                                Snackbar.make(
                                    binding.root,
                                    getString(R.string.falha_na_autenticacao),
                                    Snackbar.LENGTH_SHORT
                                ).apply {
                                    this.setBackgroundTint(Color.RED)
                                    this.setTextColor(Color.WHITE)
                                    this.show()
                                }
                            }
                        }
                } ?: run {
                    Snackbar.make(
                        binding.root,
                        getString(R.string.nao_foi_possivel_deletar_o_usuario_nao_logado),
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
        binding.senha.setBackgroundResource(R.drawable.shape_edit_text_preto)
        binding.botaoAutenticar.setBackgroundResource(R.drawable.shape_botao_preto)
    }

    private fun applyLightTheme() {
        binding.view.setBackgroundResource(R.drawable.container_background_preto)
        binding.email.setBackgroundResource(R.drawable.shape_edit_text_branco)
        binding.senha.setBackgroundResource(R.drawable.shape_edit_text_branco)
        binding.botaoAutenticar.setBackgroundResource(R.drawable.shape_botao_branco)
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