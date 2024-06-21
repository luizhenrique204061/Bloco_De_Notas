package com.olamundo.blocodenotas.ui.fragmento_tela_principal

import Adapter.ListaNotasAdapter
import DB.DB
import Modelo.Notas
import Modelo.Tarefa
import Room.AppDataBase
import Room.NotaDao
import Room.TarefaDao
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = binding.recyclerview
        // mainActivity.setSupportActionBar(binding.toolbar)
        loadTheme()
        textViewSemAnotacoes = binding.semAnotacoes



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
                } else {
                    binding.semAnotacoes.visibility = View.GONE
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
    }

    private fun applyLightTheme() {
        binding.textoCriarAnotacao.setBackgroundResource(R.drawable.shape_texto_light)
        binding.textoCriarListaTarefas.setBackgroundResource(R.drawable.shape_texto_light)
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