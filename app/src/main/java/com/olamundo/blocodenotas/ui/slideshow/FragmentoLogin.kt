package com.olamundo.blocodenotas.ui.slideshow

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.olamundo.blocodenotas.R
import com.olamundo.blocodenotas.databinding.FragmentoLoginBinding

class FragmentoLogin : Fragment() {

    private var _binding: FragmentoLoginBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentoLoginBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadTheme()
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
        binding.botaoEntrar.setBackgroundResource(R.drawable.shape_botao_preto)
    }

    private fun applyLightTheme() {
        binding.view.setBackgroundResource(R.drawable.container_background_preto)
        binding.email.setBackgroundResource(R.drawable.shape_edit_text_branco)
        binding.senha.setBackgroundResource(R.drawable.shape_edit_text_branco)
        binding.botaoEntrar.setBackgroundResource(R.drawable.shape_botao_branco)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}