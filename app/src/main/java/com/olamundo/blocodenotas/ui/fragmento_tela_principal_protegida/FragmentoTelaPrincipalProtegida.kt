package com.olamundo.blocodenotas.ui.fragmento_tela_principal_protegida

import Adapter.NotasProtegidasAdapter
import DB.DB
import Modelo.NotasProtegidas
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.olamundo.blocodenotas.CriarAnotacaoProtegida
import com.olamundo.blocodenotas.R
import com.olamundo.blocodenotas.TelaPrincipalProtegida
import com.olamundo.blocodenotas.databinding.FragmentoTelaPrincipalProtegidaBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.random.Random

class FragmentoTelaPrincipalProtegida : Fragment() {

    private var _binding: FragmentoTelaPrincipalProtegidaBinding? = null
    private val listaNotasProtegidas: MutableList<NotasProtegidas> = mutableListOf()
    private lateinit var onBackPressedCallback: OnBackPressedCallback
    private lateinit var mainActivity: TelaPrincipalProtegida
    val db = DB()
    private lateinit var textViewSemAnotacoes: TextView
    private lateinit var adapterNotacoesProtegidas: NotasProtegidasAdapter
    val scope = CoroutineScope(Dispatchers.IO)

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentoTelaPrincipalProtegidaBinding.inflate(inflater, container, false)
        mainActivity = requireActivity() as TelaPrincipalProtegida
        carregarLocalidade()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = binding.recyclerview
        loadTheme()
        textViewSemAnotacoes = binding.semAnotacoes

        adapterNotacoesProtegidas = NotasProtegidasAdapter(
            requireContext(),
            listaNotasProtegidas,
            object : NotasProtegidasAdapter.OnItemSelectedListener {
                override fun onItemSelected(selectedItemCount: Int) {
                    if (selectedItemCount == 0) {
                        mainActivity.toggleToolbarVisibility(!adapterNotacoesProtegidas.isLongClick())
                    }
                }

                override fun onItemLongClicked() {
                    mainActivity.toggleToolbarVisibility(false)
                    binding.toolbar.visibility = View.VISIBLE
                    binding.compartilhar.setOnClickListener {
                        scope.launch {
                            compartilharNotasSelecionadas()
                        }
                    }
                    binding.deletar.setOnClickListener {
                        scope.launch {
                            deletarNotasSelecionadas()
                        }
                    }
                }

                override fun updateSelectedItemCount(selectedItemCount: Int) {
                    Log.i("Contando", "$selectedItemCount")
                    binding.toolbar.title =
                        getString(R.string.itens_selecionados, selectedItemCount.toString())
                }
            })

        recyclerView.adapter = adapterNotacoesProtegidas

        binding.fabPrincipal.setOnClickListener {
            val slideAnimation = if (binding.fabCriarAnotacao.visibility == View.VISIBLE) {
                AnimationUtils.loadAnimation(context, R.anim.slide_down)
            } else {
                AnimationUtils.loadAnimation(context, R.anim.slide_up)
            }
            binding.fabCriarAnotacao.startAnimation(slideAnimation)
            binding.fabCriarAnotacao.visibility =
                if (binding.fabCriarAnotacao.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            binding.textoCriarAnotacao.startAnimation(slideAnimation)
            binding.textoCriarAnotacao.visibility =
                if (binding.textoCriarAnotacao.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        binding.fabCriarAnotacao.setOnClickListener {
            startActivity(Intent(requireContext(), CriarAnotacaoProtegida::class.java))
            binding.fabCriarAnotacao.visibility =
                if (binding.fabCriarAnotacao.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            binding.textoCriarAnotacao.visibility =
                if (binding.textoCriarAnotacao.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (adapterNotacoesProtegidas.isSelecaoAtiva()) {
                    adapterNotacoesProtegidas.desativarModoSelecao()
                    mainActivity.toggleToolbarVisibility(true)
                    binding.toolbar.visibility = View.GONE
                } else {
                    isEnabled = false
                    requireActivity().onBackPressed()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            onBackPressedCallback
        )
    }

    override fun onResume() {
        db.obterAnotacoesProtegidas(
            listaNotasProtegidas,
            adapterNotacoesProtegidas,
            textViewSemAnotacoes
        )
        super.onResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadTheme() {
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        when (currentNightMode) {
            Configuration.UI_MODE_NIGHT_YES -> applyDarkTheme()
            Configuration.UI_MODE_NIGHT_NO -> applyLightTheme()
        }
    }

    private fun applyDarkTheme() {
        binding.textoCriarAnotacao.setBackgroundResource(R.drawable.shape_texto_dark)
    }

    private fun applyLightTheme() {
        binding.textoCriarAnotacao.setBackgroundResource(R.drawable.shape_texto_light)
    }

    private suspend fun compartilharNotasSelecionadas() {
        Log.i("Clicando", "Compartilhar")
        withContext(Dispatchers.IO) {
            val notasSelecionadas =
                adapterNotacoesProtegidas.listaNotasAdapterProtegidas.filter { it.isChecked }
            if (notasSelecionadas.isEmpty()) return@withContext

            val zipFile = File(
                requireContext().filesDir,
                "${getString(R.string.nome_anotacoes_selecionadas)}.zip"
            )
            val zipOutputStream = ZipOutputStream(FileOutputStream(zipFile))

            notasSelecionadas.forEach { nota ->
                val numeroAleatorio = Random.nextLong(1000000000000000000)
                val nomeArquivo = "${numeroAleatorio}_${nota.titulo}.txt"
                val arquivo = File(requireContext().filesDir, nomeArquivo)
                arquivo.writeText("${nota.titulo}\n${nota.descricao}")

                val zipEntry = ZipEntry(nomeArquivo)
                zipOutputStream.putNextEntry(zipEntry)
                val inputStream = FileInputStream(arquivo)
                inputStream.copyTo(zipOutputStream)
                inputStream.close()
            }

            zipOutputStream.close()

            val uri = FileProvider.getUriForFile(
                requireContext(),
                "com.olamundo.blocodenotas.fileprovider",
                zipFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Confira as notas selecionadas")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            withContext(Dispatchers.Main) {
                startActivity(Intent.createChooser(intent, "Compartilhar notas via"))
            }
        }
    }

    private suspend fun deletarNotasSelecionadas() {
        withContext(Dispatchers.IO) {
            val notasSelecionadas =
                adapterNotacoesProtegidas.listaNotasAdapterProtegidas.filter { it.isChecked }
            if (notasSelecionadas.isEmpty()) {
                Log.i("DeletarNotas", "Nenhuma nota selecionada para exclusão")
                return@withContext
            }

            val currentUser = FirebaseAuth.getInstance().currentUser

            currentUser?.let {
                notasSelecionadas.forEach { nota ->
                    Log.i("DeletarNotas", "Excluindo nota com ID: ${nota.anotacaoId}")
                    db.excluirAnotacoesProtegidas(nota.anotacaoId)
                }
            } ?: run {
                Log.i("DeletarNotas", "Usuário não está logado")
            }
            withContext(Dispatchers.Main) {
                db.obterAnotacoesProtegidas(
                    listaNotasProtegidas,
                    adapterNotacoesProtegidas,
                    textViewSemAnotacoes
                )
                adapterNotacoesProtegidas.desativarModoSelecao()
                mainActivity.toggleToolbarVisibility(true)
                binding.toolbar.visibility = View.GONE
            }
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
        val preferences = requireContext().getSharedPreferences("config_linguagens", MODE_PRIVATE)
        val localidadeDoDispositivo = Locale.getDefault().language
        val linguagem = preferences.getString("minha_linguagem", localidadeDoDispositivo)
        if (linguagem != null) {
            selecionarIdioma(linguagem)
        }
    }
}