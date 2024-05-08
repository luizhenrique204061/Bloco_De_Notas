package com.olamundo.blocodenotas.ui.home

import Adapter.ListaNotasAdapter
import Modelo.Notas
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.olamundo.blocodenotas.CriarNota
import com.olamundo.blocodenotas.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private lateinit var floatingActionButton: FloatingActionButton
    private lateinit var adapter: ListaNotasAdapter
    val listaNotas: MutableList<Notas> = mutableListOf()

    // This property is only valid between onCreateView and
    // onDestroyView.
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

        adapter = ListaNotasAdapter(requireContext(), listaNotas)
        recyclerView.adapter = adapter

        floatingActionButton.setOnClickListener {
            Intent(requireContext(), CriarNota::class.java).apply {
                startActivity(this)
            }
        }


        /*
        val notas = mutableListOf(
            Notas(0, "Devoe", "Clifford", System.currentTimeMillis()),
            Notas(1, "Messiê", "ôio", System.currentTimeMillis()),
            Notas(2, "Rodera", "Zé", System.currentTimeMillis()),
        )

         */

        //listaNotas.addAll(notas)

        adapter.notifyDataSetChanged()

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}