package com.olamundo.blocodenotas

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.olamundo.blocodenotas.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)


        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        auth = FirebaseAuth.getInstance()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_tela_principal, R.id.nav_tarefas, R.id.nav_tela_login
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

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
                else -> false
            }
            true
        }

        handleNavigation(intent)
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

        // Deslogar do Google
        googleSignInClient.signOut().addOnCompleteListener(this) { tarefa ->
            // Atualize a UI após o logout, se necessário

            if (tarefa.isSuccessful) {
                Snackbar.make(binding.root, getString(R.string.usuario_deslogado_google_sucesso), Snackbar.LENGTH_SHORT).apply {
                    this.setTextColor(Color.WHITE)
                    this.setBackgroundTint(Color.RED)
                    this.show()
                }
            } else {
                Snackbar.make(binding.root, getString(R.string.falha_deslogar_usuario_google), Snackbar.LENGTH_SHORT).apply {
                    this.setTextColor(Color.WHITE)
                    this.setBackgroundTint(Color.RED)
                    this.show()
                }
            }

            // Navegar para a tela principal
            val navController = findNavController(R.id.nav_host_fragment_content_main)
            navController.navigate(R.id.nav_tela_principal)

            // Atualizar o estado do item selecionado no NavigationView
            val navView: NavigationView = binding.navView
            navView.setCheckedItem(R.id.nav_tela_principal)
        }
    }
}