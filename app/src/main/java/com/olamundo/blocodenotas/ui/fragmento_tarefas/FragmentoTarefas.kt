package com.olamundo.blocodenotas.ui.fragmento_tarefas

import Adapter.TarefasAdapterTelaPrincipal
import DB.DB
import Modelo.Tarefa
import Room.AppDataBase
import Room.TarefaDao
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.olamundo.blocodenotas.CriarNota
import com.olamundo.blocodenotas.CriarTarefa
import com.olamundo.blocodenotas.MainActivity
import com.olamundo.blocodenotas.R
import com.olamundo.blocodenotas.databinding.DialogExclusaoTarefasSelecionadasBinding
import com.olamundo.blocodenotas.databinding.FragmentoTarefasBinding
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

class FragmentoTarefas : Fragment() {

    private var _binding: FragmentoTarefasBinding? = null
    private lateinit var bancoDeDadosTarefa: TarefaDao
    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var adapterTarefas: TarefasAdapterTelaPrincipal
    private val listaTarefas: MutableList<Tarefa> = mutableListOf()
    private lateinit var onBackPressedCallback: OnBackPressedCallback
    private lateinit var mainActivity: MainActivity
    val db = DB()
    private lateinit var textViewSemTarefas: TextView
    private lateinit var textViewSemCorrespondencia: TextView

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
        carregarLocalidade()
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerViewTarefas = binding.recyclerviewTarefas
        // mainActivity.setSupportActionBar(binding.toolbar)
        loadTheme()
        textViewSemTarefas = binding.semTarefas

        textViewSemCorrespondencia = binding.nenhumaCorrespondencia


        // Definindo a cor de seleção do texto para verde
        val greenColor =
            requireContext().getColor(R.color.verde_claro) // Certifique-se de ter definido a cor verde no colors.xml
        binding.digiteParaBuscar.highlightColor = greenColor

        // Adicionando TextWatcher para monitorar mudanças no campo de busca
        binding.digiteParaBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val buscar = s.toString()
                scope.launch {
                    val buscarNotacoesRoom = if (buscar.isNotEmpty()) {
                        bancoDeDadosTarefa.buscarPorPalavraChave(buscar)
                    } else {
                        bancoDeDadosTarefa.buscarTodas()
                    }

                    Log.i("BuscarNotacoesRoom", buscarNotacoesRoom.toString())

                    // Limpar e atualizar a lista no adapter
                    withContext(Dispatchers.Main) {
                        adapterTarefas.listaTarefasTelaPrincipal.clear()
                        adapterTarefas.listaTarefasTelaPrincipal.addAll(buscarNotacoesRoom)
                        adapterTarefas.notifyDataSetChanged()

                        // Atualizar a visibilidade do "semAnotacoes"
                        if (adapterTarefas.listaTarefasTelaPrincipal.isEmpty()) {
                            binding.nenhumaCorrespondencia.visibility = View.VISIBLE
                            binding.semTarefas.visibility = View.GONE
                        } else {
                            binding.nenhumaCorrespondencia.visibility = View.GONE
                        }
                    }
                }
            }
        })

        //O código abaixo faz com que o android:dranwerRight seja clicável
//        val editTextBuscar = binding.digiteParaBuscar
//
//        // Configurando o OnTouchListener
//        editTextBuscar.setOnTouchListener { v, event ->
//            if (event.action == MotionEvent.ACTION_UP) {
//                if (event.rawX >= (editTextBuscar.right - editTextBuscar.compoundDrawables[2].bounds.width())) {
//                    // Limpar o texto do EditText
//                    binding.digiteParaBuscar.setText("")
//                    recolherTeclado()
//
//                    // Buscar todas as notas novamente e atualizar a visibilidade da mensagem
//                    scope.launch {
//                        val buscarTarefaRoom = bancoDeDadosTarefa.buscarTodas()
//
//                        withContext(Dispatchers.Main) {
//                            adapterTarefas.listaTarefasTelaPrincipal.clear()
//                            adapterTarefas.listaTarefasTelaPrincipal.addAll(buscarTarefaRoom)
//                            adapterTarefas.notifyDataSetChanged()
//
//                            // Atualizar a visibilidade do "semAnotacoes" e "nenhumaCorrespondencia"
//                            if (adapterTarefas.listaTarefasTelaPrincipal.isEmpty()) {
//                                binding.semTarefas.visibility = View.VISIBLE
//                                binding.nenhumaCorrespondencia.visibility = View.GONE
//                            } else {
//                                binding.semTarefas.visibility = View.GONE
//                                binding.nenhumaCorrespondencia.visibility = View.GONE
//                            }
//                        }
//                    }
//                    return@setOnTouchListener true
//                }
//            }
//            false
//        }

        binding.apagarPesquisa.setOnClickListener {
            binding.digiteParaBuscar.setText("")
            recolherTeclado()

            // Buscar todas as notas novamente e atualizar a visibilidade da mensagem
            scope.launch {
                val buscarTarefaRoom = bancoDeDadosTarefa.buscarTodas()

                withContext(Dispatchers.Main) {
                    adapterTarefas.listaTarefasTelaPrincipal.clear()
                    adapterTarefas.listaTarefasTelaPrincipal.addAll(buscarTarefaRoom)
                    adapterTarefas.notifyDataSetChanged()

                    // Atualizar a visibilidade do "semAnotacoes" e "nenhumaCorrespondencia"
                    if (adapterTarefas.listaTarefasTelaPrincipal.isEmpty()) {
                        binding.semTarefas.visibility = View.VISIBLE
                        binding.nenhumaCorrespondencia.visibility = View.GONE
                    } else {
                        binding.semTarefas.visibility = View.GONE
                        binding.nenhumaCorrespondencia.visibility = View.GONE
                    }
                }
            }
        }

        adapterTarefas = TarefasAdapterTelaPrincipal(
            requireContext(),
            listaTarefas,
            object : TarefasAdapterTelaPrincipal.OnItemSelectedListener {
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

        // Adicione um OnClickListener ao FloatingActionButton para iniciar a atividade CriarTarefa
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

        binding.digiteParaBuscar.setText("")

        scope.launch {
            // Buscar anotações e tarefas do banco de dados
            val buscarTarefasRoom = bancoDeDadosTarefa.buscarTodas()
            Log.i("BuscandoRoom", buscarTarefasRoom.toString())

            db.obterTarefasDoUsuario(
                requireContext(),
                listaTarefas,
                adapterTarefas,
                textViewSemTarefas
            )


            // Limpar e atualizar a lista no adapter
            withContext(Dispatchers.Main) {
                adapterTarefas.listaTarefasTelaPrincipal.clear()
                adapterTarefas.listaTarefasTelaPrincipal.addAll(buscarTarefasRoom)
                adapterTarefas.notifyDataSetChanged()

                // Atualizar a visibilidade do "semAnotacoes"
                if (adapterTarefas.listaTarefasTelaPrincipal.isEmpty()) {
                    binding.semTarefas.visibility = View.VISIBLE
                    binding.nenhumaCorrespondencia.visibility = View.GONE
                } else {
                    binding.semTarefas.visibility = View.GONE
                    binding.nenhumaCorrespondencia.visibility = View.GONE
                }
            }

            salvarTarefasNoFirebase(buscarTarefasRoom)
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
                    }, 3000)
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
        binding.layoutSecundario.setBackgroundResource(R.drawable.background_buscar_branco)
        binding.digiteParaBuscar.setTextColor(Color.BLACK)
        binding.digiteParaBuscar.setHintTextColor(Color.BLACK)
    }

    private fun applyLightTheme() {
        binding.textoCriarAnotacao.setBackgroundResource(R.drawable.shape_texto_light)
        binding.textoCriarListaTarefas.setBackgroundResource(R.drawable.shape_texto_light)
        binding.layoutSecundario.setBackgroundResource(R.drawable.background_buscar_azul_claro)
        binding.digiteParaBuscar.setTextColor(Color.BLACK)
        binding.digiteParaBuscar.setHintTextColor(Color.BLACK)
    }

    private fun recolherTeclado() {
        val inputMethodManager =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(requireView().windowToken, 0)
    }

    private suspend fun deletarTarefasSelecionadas() {
        withContext(Dispatchers.Main) {
            val dialogBinding = DialogExclusaoTarefasSelecionadasBinding.inflate(layoutInflater)
            val exibirDialog = AlertDialog.Builder(requireContext())
                .setView(dialogBinding.root)
                .setCancelable(false)
                .create() // Cria o AlertDialog, mas não o mostra ainda

            // Configura o fundo do diálogo como transparente
            exibirDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            exibirDialog.show() // Mostra o AlertDialog

            dialogBinding.botaoCancelar.setOnClickListener {
                exibirDialog.dismiss()
                mainActivity.toggleToolbarVisibility(true)
                binding.toolbar.visibility = View.GONE
                adapterTarefas.desativarModoSelecao()
            }

            dialogBinding.botaoProsseguir.setOnClickListener {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        val notasSelecionadas =
                            adapterTarefas.listaTarefasTelaPrincipal.filter { it.isChecked }
                        if (notasSelecionadas.isEmpty()) {
                            Log.i("DeletarNotas", "Nenhuma nota selecionada para exclusão")
                            return@withContext
                        }

                        val currentUser = FirebaseAuth.getInstance().currentUser

                        notasSelecionadas.forEach { tarefas ->
                            Log.i("DeletarNotas", "Excluindo nota com ID: ${tarefas.id}")

                            // Exclui do Firebase se o usuário estiver logado
                            if (currentUser != null) {
                                val excluiuFirebase = db.excluirTarefasUsuario(tarefas.id)
                                if (excluiuFirebase) {
                                    // Se excluiu com sucesso do Firebase, exclui do Room
                                    bancoDeDadosTarefa.remover(tarefas.id)
                                }
                            } else {
                                // Se o usuário não estiver logado, apenas exclui do Room
                                bancoDeDadosTarefa.remover(tarefas.id)
                            }
                        }

                        // Atualiza a lista de notas exibida após a exclusão
                        val buscarNotacoes = bancoDeDadosTarefa.buscarTodas()
                        adapterTarefas.listaTarefasTelaPrincipal.clear()
                        adapterTarefas.listaTarefasTelaPrincipal.addAll(buscarNotacoes)

                        // Notifica o adapter sobre as mudanças
                        withContext(Dispatchers.Main) {
                            adapterTarefas.notifyDataSetChanged()
                            db.obterTarefasDoUsuario(
                                requireContext(),
                                listaTarefas,
                                adapterTarefas,
                                textViewSemTarefas
                            )
                            if (buscarNotacoes.isEmpty()) {
                                binding.semTarefas.visibility = View.VISIBLE
                            } else {
                                binding.semTarefas.visibility = View.GONE
                            }
                            mainActivity.toggleToolbarVisibility(true)
                            binding.toolbar.visibility = View.GONE
                            adapterTarefas.desativarModoSelecao()
                        }
                    }

                    withContext(Dispatchers.Main) {
                        exibirDialog.dismiss()
                    }
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

            val zipFile = File(
                requireContext().filesDir,
                "${getString(R.string.nome_tarefas_selecioandas)}.zip"
            )
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
                //O código abaixo é o código original
                //startActivity(Intent.createChooser(intent, "Compartilhar notas via"))
                startActivityForResult(
                    Intent.createChooser(intent, "compartilhar tarefas via"),
                    COMPARTILHAR_TAREFAS_REQUEST_CODE
                )
                mainActivity.toggleToolbarVisibility(true)
                binding.toolbar.visibility = View.GONE
            }
        }
    }

    //Esse código aguarda o resultado do compartilhamento
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == COMPARTILHAR_TAREFAS_REQUEST_CODE) {
            // Manter os itens selecionados marcados até que o usuário retorne ao app.
            adapterTarefas.listaTarefasTelaPrincipal.forEach { it.isChecked = false }
            adapterTarefas.notifyDataSetChanged()
            adapterTarefas.desativarModoSelecao()
        }
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

    companion object {
        private const val COMPARTILHAR_TAREFAS_REQUEST_CODE = 1001
    }
}