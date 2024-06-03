package com.olamundo.blocodenotas.ui.fragmento_definicoes_da_conta_protegida

import DB.DB
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.fragment.app.Fragment
import com.olamundo.blocodenotas.AlterarNomeUsuario
import com.olamundo.blocodenotas.ExcluirConta
import com.olamundo.blocodenotas.R
import com.olamundo.blocodenotas.RedefinirSenha
import com.olamundo.blocodenotas.databinding.FragmentoDefinicoesDaContaProtegidaBinding
import java.util.Locale

class FragmentoDefinicoesDaContaProtegida : Fragment() {

    private var _binding: FragmentoDefinicoesDaContaProtegidaBinding? = null
    val db = DB()

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentoDefinicoesDaContaProtegidaBinding.inflate(inflater, container, false)
        carregarLocalidade()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadTheme()


        val exibirNomeUsuario = binding.mostrarUsuario
        val exibirEmail = binding.mostrarEmail

        db.recuperarNomeUsuarioEmailFragmento(exibirNomeUsuario, exibirEmail)

        binding.botaoRedefinirSenha.setOnClickListener {
            Intent(requireContext(), RedefinirSenha::class.java).apply {
                startActivity(this)
            }
        }

        binding.botaoAlterarNomeDoUsuario.setOnClickListener {
            Intent(requireContext(), AlterarNomeUsuario::class.java).apply {
                startActivity(this)
            }
        }

        binding.botaoExcluirConta.setOnClickListener {
            Intent(requireContext(), ExcluirConta::class.java).apply {
                startActivity(this)
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
        binding.botaoRedefinirSenha.setBackgroundResource(R.drawable.shape_botao_branco)
        binding.botaoAlterarNomeDoUsuario.setBackgroundResource(R.drawable.shape_botao_branco)
    }

    private fun applyLightTheme() {
        binding.botaoRedefinirSenha.setBackgroundResource(R.drawable.shape_botao_preto)
        binding.botaoAlterarNomeDoUsuario.setBackgroundResource(R.drawable.shape_botao_preto)
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun selecionarIdioma(linguagem: String) {
        val localidade = Locale(linguagem)
        Locale.setDefault(localidade)

        val configuration = resources.configuration
        configuration.setLocale(localidade)
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }

    private fun carregarLocalidade() {
        val preferences = requireContext().getSharedPreferences("config_linguagens", MODE_PRIVATE)
        val linguagem = preferences.getString("minha_linguagem", "")
        if (linguagem != null) {
            selecionarIdioma(linguagem)
        }
    }
}