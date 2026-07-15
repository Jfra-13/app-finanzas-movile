package com.example.finanzas_independientes_app.presentation.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.finanzas_independientes_app.core.session.SessionManager
import com.example.finanzas_independientes_app.databinding.ActivityLoginBinding
import com.example.finanzas_independientes_app.presentation.business.SelectBusinessActivity
import com.example.finanzas_independientes_app.presentation.dashboard.DashboardActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    @Inject lateinit var sessionManager: SessionManager

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by lazy { ViewModelProvider(this)[LoginViewModel::class.java] }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            val email = binding.etLoginEmail.text.toString()
            val pass = binding.etLoginPassword.text.toString()
            viewModel.iniciarSesion(email, pass)
        }

        binding.tvIrARegistro.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        lifecycleScope.launch {
            viewModel.mensajeUI.collect { mensaje ->
                if (mensaje != null) {
                    Toast.makeText(this@LoginActivity, mensaje, Toast.LENGTH_SHORT).show()
                    viewModel.limpiarMensaje()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.loginExitoso.collect { usuarioId ->
                if (usuarioId != null) {
                    if (viewModel.cuentaReactivada.value) {
                        showReactivadaDialog()
                    } else {
                        goToNextScreen()
                    }
                }
            }
        }
    }

    // Login within the 30-day grace window reactivated a deleted account; tell the
    // user before continuing. Re-deleting, if they still want it, lives in Account.
    private fun showReactivadaDialog() {
        AlertDialog.Builder(this)
            .setTitle("Cuenta reactivada")
            .setMessage(
                "Tu cuenta estaba pendiente de eliminación y quedó reactivada con " +
                    "todos tus datos."
            )
            .setCancelable(false)
            .setPositiveButton("Entendido") { _, _ -> goToNextScreen() }
            .show()
    }

    private fun goToNextScreen() {
        val intent = if (sessionManager.tipoNegocio != null) {
            Intent(this, DashboardActivity::class.java)
        } else {
            Intent(this, SelectBusinessActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}
