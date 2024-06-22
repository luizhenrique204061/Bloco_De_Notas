package com.olamundo.blocodenotas.ui.fragmento_tela_principal

import Adapter.ListaNotasAdapter
import DB.DB
import Modelo.Notas
import Modelo.Tarefa
import Room.AppDataBase
import Room.NotaDao
import Room.TarefaDao
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.auth.FirebaseAuth
import com.olamundo.blocodenotas.CriarNota
import com.olamundo.blocodenotas.CriarTarefa
import com.olamundo.blocodenotas.MainActivity
import com.olamundo.blocodenotas.R
import com.olamundo.blocodenotas.databinding.FragmentoTelaPrincipalBinding
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

class FragmentoTelaPrincipal : Fragment() {

    private var _binding: FragmentoTelaPrincipalBinding? = null
    private lateinit var bancoDeDados: NotaDao
    private lateinit var bancoDeDadosTarefa: TarefaDao
    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var adapterNotacoes: ListaNotasAdapter
    private val listaNotas: MutableList<Notas> = mutableListOf()
    private lateinit var onBackPressedCallback: OnBackPressedCallback
    private lateinit var mainActivity: MainActivity
    val db = DB()
    private lateinit var textViewSemAnotacoes: TextView
    private lateinit var textViewSemCorrespondencia: TextView
    private lateinit var appUpdateManager: AppUpdateManager
    private var mInterstitialAd: InterstitialAd? = null

    // This property is only valid between onCreateView and onDestroyView
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentoTelaPrincipalBinding.inflate(inflater, container, false)
        mainActivity = requireActivity() as MainActivity
        carregarLocalidade()
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicialização do AppUpdateManager
        appUpdateManager = AppUpdateManagerFactory.create(requireContext())

        // Verificação de atualização
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                // Iniciar o processo de atualização
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.IMMEDIATE,
                    requireActivity(),
                    REQUEST_CODE_UPDATE
                )
            }
        }

        // Definindo a cor de seleção do texto para verde
        val greenColor = requireContext().getColor(R.color.verde_claro) // Certifique-se de ter definido a cor verde no colors.xml
        binding.digiteParaBuscar.highlightColor = greenColor

       // carregarAnuncioTelaInteira()

        val recyclerView = binding.recyclerview
        // mainActivity.setSupportActionBar(binding.toolbar)
        loadTheme()
        textViewSemAnotacoes = binding.semAnotacoes

        textViewSemCorrespondencia = binding.nenhumaCorrespondencia

        // Adicionando TextWatcher para monitorar mudanças no campo de busca
        binding.digiteParaBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val buscar = s.toString()
                scope.launch {
                    val buscarNotacoesRoom = if (buscar.isNotEmpty()) {
                        bancoDeDados.buscarPorPalavraChave(buscar)
                    } else {
                        bancoDeDados.buscarTodas()
                    }

                    Log.i("BuscarNotacoesRoom", buscarNotacoesRoom.toString())

                    // Limpar e atualizar a lista no adapter
                    withContext(Dispatchers.Main) {
                        adapterNotacoes.listaNotas.clear()
                        adapterNotacoes.listaNotas.addAll(buscarNotacoesRoom)
                        adapterNotacoes.notifyDataSetChanged()

                        // Atualizar a visibilidade do "semAnotacoes"
                        if (adapterNotacoes.listaNotas.isEmpty()) {
                            binding.nenhumaCorrespondencia.visibility = View.VISIBLE
                            binding.semAnotacoes.visibility = View.GONE
                        } else {
                            binding.nenhumaCorrespondencia.visibility = View.GONE
                        }
                    }
                }
            }
        })


        val editTextBuscar = binding.digiteParaBuscar

        // Configurando o OnTouchListener
        editTextBuscar.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (editTextBuscar.right - editTextBuscar.compoundDrawables[2].bounds.width())) {
                    // Limpar o texto do EditText
                    editTextBuscar.setText("")
                    recolherTeclado()

                    // Buscar todas as notas novamente e atualizar a visibilidade da mensagem
                    scope.launch {
                        val buscarNotacoesRoom = bancoDeDados.buscarTodas()

                        withContext(Dispatchers.Main) {
                            adapterNotacoes.listaNotas.clear()
                            adapterNotacoes.listaNotas.addAll(buscarNotacoesRoom)
                            adapterNotacoes.notifyDataSetChanged()

                            // Atualizar a visibilidade do "semAnotacoes" e "nenhumaCorrespondencia"
                            if (adapterNotacoes.listaNotas.isEmpty()) {
                                binding.semAnotacoes.visibility = View.VISIBLE
                                binding.nenhumaCorrespondencia.visibility = View.GONE
                            } else {
                                binding.semAnotacoes.visibility = View.GONE
                                binding.nenhumaCorrespondencia.visibility = View.GONE
                            }
                        }
                    }
                    return@setOnTouchListener true
                }
            }
            false
        }

//        binding.apagarPesquisa.setOnClickListener {
//            binding.digiteParaBuscar.setText("")
//            recolherTeclado()
//
//            // Buscar todas as notas novamente e atualizar a visibilidade da mensagem
//            scope.launch {
//                val buscarNotacoesRoom = bancoDeDados.buscarTodas()
//
//                withContext(Dispatchers.Main) {
//                    adapterNotacoes.listaNotas.clear()
//                    adapterNotacoes.listaNotas.addAll(buscarNotacoesRoom)
//                    adapterNotacoes.notifyDataSetChanged()
//
//                    // Atualizar a visibilidade do "semAnotacoes" e "nenhumaCorrespondencia"
//                    if (adapterNotacoes.listaNotas.isEmpty()) {
//                        binding.semAnotacoes.visibility = View.VISIBLE
//                        binding.nenhumaCorrespondencia.visibility = View.GONE
//                    } else {
//                        binding.semAnotacoes.visibility = View.GONE
//                        binding.nenhumaCorrespondencia.visibility = View.GONE
//                    }
//                }
//            }
//        }

        adapterNotacoes = ListaNotasAdapter(
            requireContext(),
            listaNotas,
            object : ListaNotasAdapter.OnItemSelectedListener {
                override fun onItemSelected(selectedItemCount: Int) {
                    // Verifica se foi um clique longo e oculta a toolbar
                    if (selectedItemCount == 0) {
                        mainActivity.toggleToolbarVisibility(!adapterNotacoes.isLongClick()) // Mostra a toolbar se não for um clique longo
                    }
                }

                override fun onItemLongClicked() {
                    // Oculta a toolbar ao iniciar o clique longo
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



        recyclerView.adapter = adapterNotacoes

        bancoDeDados = AppDataBase.getInstance(requireContext()).NotaDao()
        bancoDeDadosTarefa = AppDataBase.getInstance(requireContext()).TarefaDao()

        binding.fabPrincipal.setOnClickListener {
            val slideAnimation = if (binding.fabCriarAnotacao.visibility == View.VISIBLE) {
                // Se o fabCriarAnotacao está visível, esconda-o com animação de slide down
                AnimationUtils.loadAnimation(context, R.anim.slide_down)
            } else {
                // Se o fabCriarAnotacao está invisível, torne-o visível com animação de slide up
                AnimationUtils.loadAnimation(context, R.anim.slide_up)
            }
            // Aplica a animação ao FloatingActionButton
            binding.fabCriarAnotacao.startAnimation(slideAnimation)
            // Alterna a visibilidade do FloatingActionButton
            binding.fabCriarAnotacao.visibility =
                if (binding.fabCriarAnotacao.visibility == View.VISIBLE) View.GONE else View.VISIBLE

            // Aplica a mesma animação ao TextView
            binding.textoCriarAnotacao.startAnimation(slideAnimation)
            // Alterna a visibilidade do TextView
            binding.textoCriarAnotacao.visibility =
                if (binding.textoCriarAnotacao.visibility == View.VISIBLE) View.GONE else View.VISIBLE

            // Aplica a animação ao FloatingActionButton
            binding.fabCriarTarefa.startAnimation(slideAnimation)
            // Alterna a visibilidade do FloatingActionButton
            binding.fabCriarTarefa.visibility =
                if (binding.fabCriarTarefa.visibility == View.VISIBLE) View.GONE else View.VISIBLE

            // Aplica a mesma animação ao TextView
            binding.textoCriarListaTarefas.startAnimation(slideAnimation)
            // Alterna a visibilidade do TextView
            binding.textoCriarListaTarefas.visibility =
                if (binding.textoCriarListaTarefas.visibility == View.VISIBLE) View.GONE else View.VISIBLE

        }

        // Adicione um OnClickListener ao FloatingActionButton para iniciar a atividade CriarNota
        binding.fabCriarAnotacao.setOnClickListener {
            startActivity(Intent(requireContext(), CriarNota::class.java))

            binding.fabCriarAnotacao.visibility =
                if (binding.fabCriarAnotacao.visibility == View.VISIBLE) View.GONE else View.VISIBLE

            binding.textoCriarAnotacao.visibility =
                if (binding.textoCriarAnotacao.visibility == View.VISIBLE) View.GONE else View.VISIBLE

            binding.fabCriarTarefa.visibility =
                if (binding.fabCriarTarefa.visibility == View.VISIBLE) View.GONE else View.VISIBLE

            binding.textoCriarListaTarefas.visibility =
                if (binding.textoCriarListaTarefas.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        binding.fabCriarTarefa.setOnClickListener {
            startActivity(Intent(requireContext(), CriarTarefa::class.java))

            binding.fabCriarAnotacao.visibility =
                if (binding.fabCriarAnotacao.visibility == View.VISIBLE) View.GONE else View.VISIBLE

            binding.textoCriarAnotacao.visibility =
                if (binding.textoCriarAnotacao.visibility == View.VISIBLE) View.GONE else View.VISIBLE

            binding.fabCriarTarefa.visibility =
                if (binding.fabCriarTarefa.visibility == View.VISIBLE) View.GONE else View.VISIBLE

            binding.textoCriarListaTarefas.visibility =
                if (binding.textoCriarListaTarefas.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (adapterNotacoes.isSelecaoAtiva()) {
                    adapterNotacoes.desativarModoSelecao()
                    // Mostra a toolbar ao pressionar o botão de voltar
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

    private fun recolherTeclado() {
        val inputMethodManager =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(requireView().windowToken, 0)
    }

    private fun carregarAnuncioTelaInteira() {
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            requireContext(),
            "ca-app-pub-2053981007263513/9501469318",
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d("MainActivity", adError?.toString()!!)
                    mInterstitialAd = null
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d("MainActivity", "Ad was loaded.")
                    mInterstitialAd = interstitialAd
                    mInterstitialAd?.show(requireActivity())
                }
            })
    }

    override fun onResume() {
        super.onResume()

        scope.launch {
            // Buscar anotações no Room
            val buscarNotacoesRoom = bancoDeDados.buscarTodas()
            val buscarTarefasRoomFragmentoTelaPrincipal = bancoDeDadosTarefa.buscarTodas()
            Log.i("BuscandoRoom", buscarNotacoesRoom.toString())

            db.obterAnotacoesDoUsuario(requireContext(), listaNotas, adapterNotacoes, textViewSemAnotacoes)


            // Limpar e atualizar a lista no adapter
            withContext(Dispatchers.Main) {
                adapterNotacoes.listaNotas.clear()
                adapterNotacoes.listaNotas.addAll(buscarNotacoesRoom)
                adapterNotacoes.notifyDataSetChanged()

                // Atualizar a visibilidade do "semAnotacoes"
                if (adapterNotacoes.listaNotas.isEmpty()) {
                    binding.semAnotacoes.visibility = View.VISIBLE
                    binding.nenhumaCorrespondencia.visibility = View.GONE
                } else {
                    binding.semAnotacoes.visibility = View.GONE
                    binding.nenhumaCorrespondencia.visibility = View.GONE
                }
            }

            salvarNotasNoFirebase(buscarNotacoesRoom)
            salvarTarefasNoFirebase(buscarTarefasRoomFragmentoTelaPrincipal)
        }
    }

    private fun salvarNotasNoFirebase(notas: MutableList<Notas>) {

        val usuarioAtual = FirebaseAuth.getInstance().currentUser

        if (usuarioAtual != null) {
            for (nota in notas) {
                db.salvarAnotacoesNaNuvem(nota.id, nota.titulo, nota.descricao, nota.data)
            }
            scope.launch {
                withContext(Dispatchers.Main) {
                    mainActivity.iniciarAnimacaoSincronizacao()
                    Handler().postDelayed({
                        mainActivity.pararAnimacao()
                    },3000)
                    /*
                    Snackbar.make(binding.root, "Backup iniciado", Snackbar.LENGTH_SHORT).apply {
                        this.setBackgroundTint(Color.parseColor("#214C06"))
                        this.setTextColor(Color.WHITE)
                        this.show()
                    }

                     */
                }
            }

        } else {
            /*
            Snackbar.make(binding.root, "Você não está logado", Snackbar.LENGTH_SHORT).apply {
                this.setBackgroundTint(Color.RED)
                this.setTextColor(Color.WHITE)
                this.show()
            }

             */
        }

    }


    private fun salvarTarefasNoFirebase(tarefas: MutableList<Tarefa>) {

        val usuarioAtual = FirebaseAuth.getInstance().currentUser

        usuarioAtual?.let {
            for (tarefa in tarefas) {
                db.salvarTarefasNaNuvem(tarefa.id, tarefa.titulo, tarefa.descricao, tarefa.data)
            }
            scope.launch {
                withContext(Dispatchers.Main) {
                    mainActivity.iniciarAnimacaoSincronizacao()
                    Handler().postDelayed({
                        mainActivity.pararAnimacao()
                    },3000)
                    /*
                    Snackbar.make(binding.root, "Backup iniciado", Snackbar.LENGTH_SHORT).apply {
                        this.setBackgroundTint(Color.parseColor("#214C06"))
                        this.setTextColor(Color.WHITE)
                        this.show()
                    }

                     */
                }
            }
        } ?: run {
            /*
            Snackbar.make(binding.root, "Você não está logado", Snackbar.LENGTH_SHORT).apply {
                this.setBackgroundTint(Color.RED)
                this.setTextColor(Color.WHITE)
                this.show()
            }

             */
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
        binding.textoCriarAnotacao.setBackgroundResource(R.drawable.shape_texto_dark)
        binding.textoCriarListaTarefas.setBackgroundResource(R.drawable.shape_texto_dark)
        binding.digiteParaBuscar.setBackgroundResource(R.drawable.background_buscar_branco)
        binding.digiteParaBuscar.setTextColor(Color.BLACK)
        binding.digiteParaBuscar.setHintTextColor(Color.BLACK)
    }

    private fun applyLightTheme() {
        binding.textoCriarAnotacao.setBackgroundResource(R.drawable.shape_texto_light)
        binding.textoCriarListaTarefas.setBackgroundResource(R.drawable.shape_texto_light)
        binding.digiteParaBuscar.setBackgroundResource(R.drawable.background_buscar_cinza)
        binding.digiteParaBuscar.setTextColor(Color.BLACK)
        binding.digiteParaBuscar.setHintTextColor(Color.BLACK)
    }


    private suspend fun compartilharNotasSelecionadas() {
        Log.i("Clicando", "Compartilhar")
        withContext(Dispatchers.IO) {
            val notasSelecionadas = adapterNotacoes.listaNotas.filter { it.isChecked }
            if (notasSelecionadas.isEmpty()) return@withContext

            val zipFile = File(requireContext().filesDir, "${getString(R.string.nome_anotacoes_selecionadas)}.zip")
            val zipOutputStream = ZipOutputStream(FileOutputStream(zipFile))

            notasSelecionadas.forEach { nota ->
                val nomeArquivo = "${nota.id}_${nota.titulo}.txt"
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
            val notasSelecionadas = adapterNotacoes.listaNotas.filter { it.isChecked }
            if (notasSelecionadas.isEmpty()) {
                Log.i("DeletarNotas", "Nenhuma nota selecionada para exclusão")
                return@withContext
            }

            val currentUser = FirebaseAuth.getInstance().currentUser

            notasSelecionadas.forEach { nota ->
                Log.i("DeletarNotas", "Excluindo nota com ID: ${nota.id}")

                // Exclui do Firebase se o usuário estiver logado
                if (currentUser != null) {
                    val excluiuFirebase = db.excluirAnotacoesUsuario(nota.id)
                    if (excluiuFirebase) {
                        // Se excluiu com sucesso do Firebase, exclui do Room
                        bancoDeDados.remover(nota.id)
                    }
                } else {
                    // Se o usuário não estiver logado, apenas exclui do Room
                    bancoDeDados.remover(nota.id)
                }
            }

            // Atualiza a lista de notas exibida após a exclusão
            val buscarNotacoes = bancoDeDados.buscarTodas()
            adapterNotacoes.listaNotas.clear()
            adapterNotacoes.listaNotas.addAll(buscarNotacoes)

            // Notifica o adapter sobre as mudanças
            withContext(Dispatchers.Main) {
                adapterNotacoes.notifyDataSetChanged()
                db.obterAnotacoesDoUsuario(requireContext(), listaNotas, adapterNotacoes, textViewSemAnotacoes)
                if (buscarNotacoes.isEmpty()) {
                    binding.semAnotacoes.visibility = View.VISIBLE
                } else {
                    binding.semAnotacoes.visibility = View.GONE
                }
                mainActivity.toggleToolbarVisibility(true)
                binding.toolbar.visibility = View.GONE
                adapterNotacoes.desativarModoSelecao()
            }
        }
    }

    private fun selecionarIdioma(linguagem: String) {
        Log.d("SelecionarIdioma", "Idioma selecionado: $linguagem")

        val localidade = Locale(linguagem)
        Locale.setDefault(localidade)

        // Obter o objeto Configuration da atividade atual
        val configuration = resources.configuration

        // Configurar a localidade para a Configuration
        configuration.setLocale(localidade)

        // Atualizar a Configuration na atividade atual
        resources.updateConfiguration(configuration, resources.displayMetrics)

        Log.d("SelecionarIdioma", "Configuração de localidade atualizada: $localidade")
    }

    private fun carregarLocalidade() {
        Log.d("CarregarLocalidade", "Carregando localidade")

        val preferences = requireContext().getSharedPreferences("config_linguagens", MODE_PRIVATE)
        val localidadeDoDispositivo = Locale.getDefault().language
        val linguagem = preferences.getString("minha_linguagem", localidadeDoDispositivo)

        Log.d("CarregarLocalidade", "Idioma do dispositivo: $localidadeDoDispositivo")
        Log.d("CarregarLocalidade", "Idioma carregado das preferências: $linguagem")

        if (linguagem != null) {
            selecionarIdioma(linguagem)
        } else {
            Log.d("CarregarLocalidade", "Nenhuma linguagem encontrada nas preferências, usando idioma do dispositivo.")
        }
    }

    companion object {
        private const val REQUEST_CODE_UPDATE = 100
    }
}