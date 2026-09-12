package com.olamundo.blocodenotas

import DB.DB
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.olamundo.blocodenotas.databinding.ActivityTelaPrincipalProtegidaBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class TelaPrincipalProtegida : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityTelaPrincipalProtegidaBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var navView: NavigationView
    val db = DB()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        carregarLocalidade()
        binding = ActivityTelaPrincipalProtegidaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val isModoEscuro = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        // Define a cor da barra de status conforme o tema
        window.statusBarColor = if (isModoEscuro) {
            ContextCompat.getColor(this, R.color.black)
        } else {
            ContextCompat.getColor(this, R.color.white)
        }

        // Controla o contraste dos ícones do sistema (escuros no tema claro, brancos no escuro)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isModoEscuro
            isAppearanceLightNavigationBars = !isModoEscuro
        }

        // Aplica o padding no container da Toolbar e conteúdo, evitando cortes
        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarTelaPrincipalProtegida.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setSupportActionBar(binding.appBarTelaPrincipalProtegida.toolbar)
        auth = FirebaseAuth.getInstance()
        navView = binding.navView

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navController = findNavController(R.id.nav_host_fragment_content_tela_principal_protegida)

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_tela_principal_protegida), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        navView.setNavigationItemSelectedListener { menuItem ->
            menuItem.isChecked = true
            drawerLayout.closeDrawers()
            when (menuItem.itemId) {
                R.id.nav_tela_principal_protegida -> navController.navigate(R.id.nav_tela_principal_protegida)
                R.id.nav_detalhes_da_conta_protegida -> navController.navigate(R.id.nav_detalhes_da_conta_protegida)
                R.id.deslogar_login_protegido -> {
                    signOut()
                }
                else -> false
            }
            true
        }
    }

    override fun onResume() {
        super.onResume()
        val headerView = navView.getHeaderView(0)
        val textView: TextView = headerView.findViewById(R.id.nome_tela_protegida)
        val nomeUsuario = getString(R.string.nome_usuario)

        db.recuperarNomeUsuario(nomeUsuario, textView, this)
    }

    private fun signOut() {
        auth.signOut()
        Log.d("TelaPrincipalProtegida", "Usuário deslogado do Firebase")

        Snackbar.make(
            binding.root,
            getString(R.string.usuario_deslogado_com_sucesso_email),
            Snackbar.LENGTH_SHORT
        ).apply {
            this.setBackgroundTint(Color.RED)
            this.setTextColor(Color.WHITE)
            this.show()
        }
        Log.d("TelaPrincipalProtegida", "Snackbar de logout mostrado")

        lifecycleScope.launch {
            delay(3000)
            Log.d("TelaPrincipalProtegida", "Navegando para MainActivity")
            startActivity(Intent(this@TelaPrincipalProtegida, MainActivity::class.java))
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_tela_principal_protegida)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    fun toggleToolbarVisibility(visibilidade: Boolean) {
        if (visibilidade) {
            supportActionBar?.show()
        } else {
            supportActionBar?.hide()
        }
    }

    private fun selecionarIdioma(linguagem: String) {
        val localidade = Locale(linguagem)
        Locale.setDefault(localidade)

        val configuration = resources.configuration
        configuration.setLocale(localidade)
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