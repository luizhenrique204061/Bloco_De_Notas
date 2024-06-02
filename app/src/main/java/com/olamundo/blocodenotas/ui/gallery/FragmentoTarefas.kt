package com.olamundo.blocodenotas.ui.gallery

import Adapter.TarefasAdapterTelaPrincipal
import Modelo.Tarefa
import Room.AppDataBase
import Room.TarefaDao
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
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.olamundo.blocodenotas.CriarNota
import com.olamundo.blocodenotas.CriarTarefa
import com.olamundo.blocodenotas.MainActivity
import com.olamundo.blocodenotas.R
import com.olamundo.blocodenotas.databinding.FragmentoTarefasBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class FragmentoTarefas : Fragment() {

    private var _binding: FragmentoTarefasBinding? = null
    private lateinit var bancoDeDadosTarefa: TarefaDao
    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var adapterTarefas: TarefasAdapterTelaPrincipal
    private val listaTarefas: MutableList<Tarefa> = mutableListOf()
    private lateinit var onBackPressedCallback: OnBackPressedCallback
    private lateinit var mainActivity: MainActivity

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {


        _binding = FragmentoTarefasBinding.inflate(inflater, container, false)
        mainActivity = requireActivity() as MainActivity
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerViewTarefas = binding.recyclerviewTarefas
        // mainActivity.setSupportActionBar(binding.toolbar)
        loadTheme()

        adapterTarefas = TarefasAdapterTelaPrincipal(requireContext(), listaTarefas, object : TarefasAdapterTelaPrincipal.OnItemSelectedListener {
            override fun onItemSelected(selectedItemCount: Int) {
                // Verifica se foi um clique longo e oculta a toolbar
                if (selectedItemCount == 0) {
                    mainActivity.toggleToolbarVisibility(!adapterTarefas.isLongClick()) // Mostra a toolbar se não for um clique longo
                }
            }

            override fun onItemLongClicked() {
                // Oculta a toolbar ao iniciar o clique longo
                mainActivity.toggleToolbarVisibility(false)
                binding.toolbar.visibility = View.VISIBLE
                binding.compartilhar.setOnClickListener {
                    scope.launch {
                        compartilharTarefasSelecionadas()
                    }

                }
                binding.deletar.setOnClickListener {
                    scope.launch {
                        deletarTarefasSelecionadas()
                    }
                }
            }

            override fun updateSelectedItemCount(selectedItemCount: Int) {
                Log.i("Contando", "$selectedItemCount")
                binding.toolbar.title =
                    getString(R.string.itens_selecionados, selectedItemCount.toString())
            }

        })

        recyclerViewTarefas.adapter = adapterTarefas

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
        }

        binding.fabCriarTarefa.setOnClickListener {
            startActivity(Intent(requireContext(), CriarTarefa::class.java))
        }

        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (adapterTarefas.isSelecaoAtiva()) {
                    adapterTarefas.desativarModoSelecao()
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
            // Buscar anotações e tarefas do banco de dados
            val buscarTarefas = bancoDeDadosTarefa.buscarTodas()


            // Limpar e atualizar a lista no adapter
            withContext(Dispatchers.Main) {
                adapterTarefas.listaTarefasTelaPrincipal.clear()
                adapterTarefas.listaTarefasTelaPrincipal.addAll(buscarTarefas)
                adapterTarefas.notifyDataSetChanged()

                // Atualizar a visibilidade do "semAnotacoes"
                if (adapterTarefas.listaTarefasTelaPrincipal.isEmpty()) {
                    binding.semAnotacoes.visibility = View.VISIBLE
                } else {
                    binding.semAnotacoes.visibility = View.GONE
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
        binding.textoCriarAnotacao.setBackgroundResource(R.drawable.shape_texto_dark)
        binding.textoCriarListaTarefas.setBackgroundResource(R.drawable.shape_texto_dark)
    }

    private fun applyLightTheme() {
        binding.textoCriarAnotacao.setBackgroundResource(R.drawable.shape_texto_light)
        binding.textoCriarListaTarefas.setBackgroundResource(R.drawable.shape_texto_light)
    }

    private suspend fun deletarTarefasSelecionadas() {
        withContext(Dispatchers.IO) {
            val tarefasSelecionadas =
                adapterTarefas.listaTarefasTelaPrincipal.filter { it.isChecked }
            if (tarefasSelecionadas.isEmpty()) {
                return@withContext
            } else {
                // Deleta cada nota selecionada individualmente
                tarefasSelecionadas.forEach { tarefa ->
                    bancoDeDadosTarefa.remover(tarefa.id)
                    withContext(Dispatchers.Main) {
                        mainActivity.toggleToolbarVisibility(true)
                        binding.toolbar.visibility = View.GONE
                        adapterTarefas.desativarModoSelecao()
                    }
                }
            }

            // Atualiza a lista de notas exibida após a exclusão
            val buscarTarefa = bancoDeDadosTarefa.buscarTodas()
            adapterTarefas.listaTarefasTelaPrincipal.clear()
            adapterTarefas.listaTarefasTelaPrincipal.addAll(buscarTarefa)

            // Notifica o adapter sobre as mudanças
            withContext(Dispatchers.Main) {
                adapterTarefas.notifyDataSetChanged()
                if (buscarTarefa.isEmpty()) {
                    binding.semAnotacoes.visibility = View.VISIBLE
                } else {
                    binding.semAnotacoes.visibility = View.GONE
                }
            }
        }
    }


    private suspend fun compartilharTarefasSelecionadas() {
        Log.i("Clicando", "Compartilhar")
        withContext(Dispatchers.IO) {
            val tarefasSelecionadas =
                adapterTarefas.listaTarefasTelaPrincipal.filter { it.isChecked }
            if (tarefasSelecionadas.isEmpty()) return@withContext

            val zipFile = File(requireContext().filesDir, "${getString(R.string.nome_tarefas_selecioandas)}.zip")
            val zipOutputStream = ZipOutputStream(FileOutputStream(zipFile))

            tarefasSelecionadas.forEach { tarefa ->
                val nomeArquivo = "${tarefa.id}_${tarefa.titulo}.txt"
                val arquivo = File(requireContext().filesDir, nomeArquivo)
                arquivo.writeText("${tarefa.titulo}\n${tarefa.descricao}")

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}