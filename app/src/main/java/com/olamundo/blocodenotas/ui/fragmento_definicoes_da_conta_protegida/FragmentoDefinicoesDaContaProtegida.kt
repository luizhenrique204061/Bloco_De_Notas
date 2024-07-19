package com.olamundo.blocodenotas.ui.fragmento_definicoes_da_conta_protegida

import DB.DB
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.fragment.app.Fragment
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.olamundo.blocodenotas.AlterarNomeUsuario
import com.olamundo.blocodenotas.ExcluirConta
import com.olamundo.blocodenotas.R
import com.olamundo.blocodenotas.RedefinirSenha
import com.olamundo.blocodenotas.databinding.FragmentoDefinicoesDaContaProtegidaBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class FragmentoDefinicoesDaContaProtegida : Fragment() {

    private var _binding: FragmentoDefinicoesDaContaProtegidaBinding? = null
    val db = DB()
    lateinit var mAdview: AdView
    val scope = CoroutineScope(Dispatchers.IO)
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
        //carregarAnuncioBanner()


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

        // Obter o objeto Configuration da atividade atual
        val configuration = resources.configuration

        // Configurar a localidade para a Configuration
        configuration.setLocale(localidade)

        // Atualizar a Configuration na atividade atual
        resources.updateConfiguration(configuration, resources.displayMetrics)

    }

    private fun carregarLocalidade() {
        val preferences = requireContext().getSharedPreferences("config_linguagens", MODE_PRIVATE)
        val localidadeDoDispositivo = Locale.getDefault().language
        val linguagem = preferences.getString("minha_linguagem", localidadeDoDispositivo)
        if (linguagem != null) {
            selecionarIdioma(linguagem)
        }
    }

    private fun carregarAnuncioBanner() {
        scope.launch {
            //Anúncio do Tipo Banner
            MobileAds.initialize(requireContext())
            val adRequest = AdRequest.Builder().build()
            withContext(Dispatchers.Main) {
                mAdview = binding.adview
                Log.i("Meu App", "Antes de carregar o anúncio")
                mAdview.loadAd(adRequest)
            }
        }
    }
}