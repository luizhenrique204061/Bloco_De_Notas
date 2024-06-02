package com.olamundo.blocodenotas.ui.slideshow

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.olamundo.blocodenotas.R
import com.olamundo.blocodenotas.databinding.FragmentoLoginBinding

class FragmentoLogin : Fragment() {

    private var _binding: FragmentoLoginBinding? = null
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth

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

        // Configurar o Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Verifique se este ID está correto no google-services.json
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
        auth = FirebaseAuth.getInstance()

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            Log.i("Login", userId)
        } else {
            Log.i("Login", "Nenhum usuário logado")
        }

        binding.botaoEntrarComGoogle.setOnClickListener {
            signInWithGoogle()
        }

        binding.botaoCadastrar.setOnClickListener {

        }

        loadTheme()
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startForResult.launch(signInIntent)
    }

    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account)
        } catch (e: ApiException) {
            Log.w("FragmentoLogin", "Google sign in failed", e)
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    Snackbar.make(binding.root, getString(R.string.usuario_logado_google_sucesso), Snackbar.LENGTH_SHORT).apply {
                        this.setBackgroundTint(Color.parseColor("#214C06"))
                        this.setTextColor(Color.WHITE)
                        this.show()
                    }

                    user?.let {
                        val userId = it.uid
                        Log.d("FragmentoLogin", "User ID: $userId")
                    }
                    // Login bem-sucedido, navegue para a próxima tela ou faça o que for necessário
                } else {
                    Log.w("FragmentoLogin", "signInWithCredential:failure", task.exception)

                    Snackbar.make(binding.root, getString(R.string.falha_login_google), Snackbar.LENGTH_SHORT).apply {
                        this.setBackgroundTint(Color.RED)
                        this.setTextColor(Color.WHITE)
                        this.show()
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
        binding.view.setBackgroundResource(R.drawable.container_backgrond_branco)
        binding.email.setBackgroundResource(R.drawable.shape_edit_text_preto)
        binding.senha.setBackgroundResource(R.drawable.shape_edit_text_preto)
        binding.botaoEntrar.setBackgroundResource(R.drawable.shape_botao_preto)
        binding.botaoEntrarComGoogle.setBackgroundResource(R.drawable.shape_botao_preto)
    }

    private fun applyLightTheme() {
        binding.view.setBackgroundResource(R.drawable.container_background_preto)
        binding.email.setBackgroundResource(R.drawable.shape_edit_text_branco)
        binding.senha.setBackgroundResource(R.drawable.shape_edit_text_branco)
        binding.botaoEntrar.setBackgroundResource(R.drawable.shape_botao_branco)
        binding.botaoEntrarComGoogle.setBackgroundResource(R.drawable.shape_botao_branco)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}