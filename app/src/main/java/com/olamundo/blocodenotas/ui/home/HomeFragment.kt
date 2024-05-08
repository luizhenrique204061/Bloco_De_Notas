package com.olamundo.blocodenotas.ui.home

import Adapter.ListaNotasAdapter
import Modelo.Notas
import Room.AppDataBase
import Room.NotaDao
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.olamundo.blocodenotas.CriarNota
import com.olamundo.blocodenotas.databinding.FragmentHomeBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private lateinit var bancoDeDados: NotaDao
    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var floatingActionButton: FloatingActionButton
    private lateinit var adapter: ListaNotasAdapter
    private val listaNotas: MutableList<Notas> = mutableListOf()
    private lateinit var onBackPressedCallback: OnBackPressedCallback

    // This property is only valid between oAnCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        floatingActionButton = binding.fabPrincipal
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = binding.recyclerview

        adapter = ListaNotasAdapter(requireContext(), listaNotas, object : ListaNotasAdapter.OnItemSelectedListener {
            override fun onItemSelected(selectedItemCount: Int) {
                Log.i("CliqueLongo", "Dando Clique Longo")
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

    override fun onDestroyView() {
        super.onDestroyView()
        onBackPressedCallback.remove()
        _binding = null
    }
}