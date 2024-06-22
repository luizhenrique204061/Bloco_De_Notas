package com.olamundo.blocodenotas

import DB.DB
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.olamundo.blocodenotas.databinding.ActivityMainBinding
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    var isAnimating: Boolean = false
    val db = DB()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        carregarLocalidade()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        auth = FirebaseAuth.getInstance()

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_tela_principal, R.id.nav_tarefas, R.id.nav_tela_login
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Defina a cor do seletor do NavigationView
        //navView.itemIconTintList = ContextCompat.getColorStateList(this, R.color.vermelho)

        val headerView = navView.getHeaderView(0)
        val textView: TextView = headerView.findViewById(R.id.nome)
        val nomeUsuario = getString(R.string.nome_usuario)

        val headerViewDeslogado = binding.navView.getHeaderView(0)
        val textoBackup: TextView = headerViewDeslogado.findViewById(R.id.aviso_backup)

        if (auth.currentUser != null) {
            textoBackup.visibility = View.GONE
        } else {
            textoBackup.visibility = View.VISIBLE
        }

        db.recuperarNomeUsuario(nomeUsuario, textView)

        // Adicionar um listener para resgatar a navegação sempre que a NavigationView for usada
        navView.setNavigationItemSelectedListener { menuItem ->
            menuItem.isChecked = true
            drawerLayout.closeDrawers()
            when (menuItem.itemId) {
                R.id.nav_tela_principal -> navController.navigate(R.id.nav_tela_principal)
                R.id.nav_tarefas -> navController.navigate(R.id.nav_tarefas)
                R.id.nav_tela_login -> navController.navigate(R.id.nav_tela_login)
                R.id.deslogar -> {
                    signOut()
                }
                R.id.nav_dados_do_usuario -> navController.navigate(R.id.nav_dados_do_usuario)
                R.id.activity_login -> {
                    irParaTelaDeLogin()
                }
                R.id.activity_idiomas -> {
                    irParaTelaDeIdiomas()
                }
                else -> false
            }
            true
        }

        handleNavigation(intent)
    }

    private fun irParaTelaDeIdiomas() {
        Log.d("MainActivity", "Iniciando TelaIdiomas")
        Intent(this, TelaIdiomas::class.java).apply {
            startActivity(this)
            binding.navView.post {
                binding.navView.setCheckedItem(R.id.nav_tela_principal)
                val navController = findNavController(R.id.nav_host_fragment_content_main)
                navController.navigate(R.id.nav_tela_principal)
            }
        }
        Log.d("MainActivity", "TelaIdiomas iniciada")
    }

    private fun irParaTelaDeLogin() {
        Intent(this, TelaLogin::class.java).apply {
            startActivity(this)
            binding.navView.post {
                binding.navView.setCheckedItem(R.id.nav_tela_principal)
                val navController = findNavController(R.id.nav_host_fragment_content_main)
                navController.navigate(R.id.nav_tela_principal)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleNavigation(intent)
    }

    private fun handleNavigation(intent: Intent?) {
        intent?.getStringExtra("navigate_to_fragment")?.let { fragment ->
            val navController = findNavController(R.id.nav_host_fragment_content_main)
            if (fragment == "FragmentoTarefas") {
                navController.navigate(R.id.nav_tarefas)
            } else if (fragment == "FragmentoLogin"){
                navController.navigate(R.id.nav_tela_login)
            }
        }
    }

    fun toggleToolbarVisibility(visibilidade: Boolean) {
        if (visibilidade) {
            supportActionBar?.show()
        } else {
            supportActionBar?.hide()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun signOut() {
        // Deslogar do Firebase
        auth.signOut()

        val headerViewDeslogado = binding.navView.getHeaderView(0)
        val textoBackup: TextView = headerViewDeslogado.findViewById(R.id.aviso_backup)

        if (auth.currentUser != null) {
            textoBackup.visibility = View.GONE
        } else {
            textoBackup.visibility = View.VISIBLE
        }

        // Navegar para a tela principal
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        navController.navigate(R.id.nav_tela_principal)

        // Definir o item nav_tela_principal como selecionado após a navegação
        binding.navView.post {
            binding.navView.setCheckedItem(R.id.nav_tela_principal)
        }

        // Mostrar Snackbar de confirmação de logout
        Snackbar.make(
            binding.root,
            getString(R.string.usuario_deslogado_com_sucesso_email),
            Snackbar.LENGTH_SHORT
        ).apply {
            this.setBackgroundTint(Color.RED)
            this.setTextColor(Color.WHITE)
            this.show()
        }

        // Limpar o nome do usuário do header da NavigationView
        val headerView = binding.navView.getHeaderView(0)
        val textView: TextView = headerView.findViewById(R.id.nome)
        textView.visibility = View.GONE
    }

    fun iniciarAnimacaoSincronizacao() {
        isAnimating = true
        if (isAnimating) {
            val sincronizarButton = binding.appBarMain.sincronizar
            sincronizarButton.visibility = View.VISIBLE
            val animation = AnimationUtils.loadAnimation(this, R.anim.rotacao)
            sincronizarButton.startAnimation(animation)
        }
    }

    fun pararAnimacao() {
        isAnimating = false
        val sincronizarButton = binding.appBarMain.sincronizar
        sincronizarButton.clearAnimation()
        sincronizarButton.visibility = View.INVISIBLE
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
        val preferences = getSharedPreferences("config_linguagens", MODE_PRIVATE)
        val localidadeDoDispositivo = Locale.getDefault().language
        val linguagem = preferences.getString("minha_linguagem", localidadeDoDispositivo)
        if (linguagem != null) {
            selecionarIdioma(linguagem)
        }
    }
}