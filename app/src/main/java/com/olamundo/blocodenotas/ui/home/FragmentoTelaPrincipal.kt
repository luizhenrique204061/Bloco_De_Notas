package com.olamundo.blocodenotas.ui.home

import Adapter.ListaNotasAdapter
import Modelo.Notas
import Room.AppDataBase
import Room.NotaDao
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.olamundo.blocodenotas.CriarNota
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
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class FragmentoTelaPrincipal : Fragment() {

    private var _binding: FragmentoTelaPrincipalBinding? = null
    private lateinit var bancoDeDados: NotaDao
    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var floatingActionButton: FloatingActionButton
    private lateinit var adapter: ListaNotasAdapter
    private val listaNotas: MutableList<Notas> = mutableListOf()
    private lateinit var onBackPressedCallback: OnBackPressedCallback
    private lateinit var mainActivity: MainActivity

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentoTelaPrincipalBinding.inflate(inflater, container, false)
        floatingActionButton = binding.fabPrincipal
        mainActivity = requireActivity() as MainActivity
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = binding.recyclerview
       // mainActivity.setSupportActionBar(binding.toolbar)

        adapter = ListaNotasAdapter(requireContext(), listaNotas, object : ListaNotasAdapter.OnItemSelectedListener {
            override fun onItemSelected(selectedItemCount: Int) {
                // Verifica se foi um clique longo e oculta a toolbar
                if (selectedItemCount == 0) {
                    mainActivity.toggleToolbarVisibility(!adapter.isLongClick()) // Mostra a toolbar se não for um clique longo
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
                binding.toolbar.title = getString(R.string.itens_selecionados, selectedItemCount.toString())
            }
        })
        recyclerView.adapter = adapter

        bancoDeDados = AppDataBase.getInstance(requireContext()).NotaDao()

        floatingActionButton.setOnClickListener {
            Intent(requireContext(), CriarNota::class.java).apply {
                startActivity(this)
            }
        }

        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (adapter.isSelecaoAtiva()) {
                    adapter.desativarModoSelecao()
                    // Mostra a toolbar ao pressionar o botão de voltar
                    mainActivity.toggleToolbarVisibility(true)
                    binding.toolbar.visibility = View.GONE
                } else {
                    isEnabled = false
                    requireActivity().onBackPressed()
                }
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)
    }

    override fun onResume() {
        scope.launch {
            val buscarNotacoes = bancoDeDados.buscarTodas()
            adapter.listaNotas.clear()
            adapter.listaNotas.addAll(buscarNotacoes)

            withContext(Dispatchers.Main) {
                adapter.notifyDataSetChanged()
                if (buscarNotacoes.isEmpty()) {
                    binding.semAnotacoes.visibility = View.VISIBLE
                } else {
                    binding.semAnotacoes.visibility = View.GONE
                }
            }
        }
        super.onResume()
    }

    private suspend fun deletarNotasSelecionadas() {
        withContext(Dispatchers.IO) {
            val notasSelecionadas = adapter.listaNotas.filter { it.isChecked }
            if (notasSelecionadas.isEmpty()) {
                return@withContext
            } else {
                // Deleta cada nota selecionada individualmente
                notasSelecionadas.forEach { nota ->
                    bancoDeDados.remover(nota.id)
                    withContext(Dispatchers.Main) {
                        mainActivity.toggleToolbarVisibility(true)
                        binding.toolbar.visibility = View.GONE
                        adapter.desativarModoSelecao()
                    }
                }
            }

            // Atualiza a lista de notas exibida após a exclusão
            val buscarNotacoes = bancoDeDados.buscarTodas()
            adapter.listaNotas.clear()
            adapter.listaNotas.addAll(buscarNotacoes)

            // Notifica o adapter sobre as mudanças
            withContext(Dispatchers.Main) {
                adapter.notifyDataSetChanged()
                if (buscarNotacoes.isEmpty()) {
                    binding.semAnotacoes.visibility = View.VISIBLE
                } else {
                    binding.semAnotacoes.visibility = View.GONE
                }
            }
        }
    }

    private suspend fun compartilharNotasSelecionadas() {
        Log.i("Clicando", "Compartilhar")
        withContext(Dispatchers.IO) {
            val notasSelecionadas = adapter.listaNotas.filter { it.isChecked }
            if (notasSelecionadas.isEmpty()) return@withContext

            val zipFile = File(requireContext().filesDir, "notas_selecionadas.zip")
            val zipOutputStream = ZipOutputStream(FileOutputStream(zipFile))

            notasSelecionadas.forEach { nota ->
                val nomeArquivo = "${nota.titulo}.txt"
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

    override fun onDestroyView() {
        super.onDestroyView()
        onBackPressedCallback.remove()
        _binding = null
    }
}