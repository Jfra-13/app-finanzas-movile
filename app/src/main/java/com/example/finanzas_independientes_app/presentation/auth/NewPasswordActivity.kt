package com.example.finanzas_independientes_app.presentation.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.finanzas_independientes_app.databinding.ActivityNewPasswordBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NewPasswordActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EMAIL = "extra_email"
        const val EXTRA_OTP = "extra_otp"
    }

    private lateinit var binding: ActivityNewPasswordBinding
    private val viewModel: NewPasswordViewModel by lazy {
        ViewModelProvider(this)[NewPasswordViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val email = intent.getStringExtra(EXTRA_EMAIL) ?: ""
        val otp = intent.getStringExtra(EXTRA_OTP) ?: ""

        binding.ivBackArrowNewPass.setOnClickListener { finish() }

        binding.btnSaveNewPassword.setOnClickListener {
            val newPassword = binding.etNewPassword.text.toString()
            val repeat = binding.etConfirmNewPassword.text.toString()
            viewModel.restablecer(email, otp, newPassword, repeat)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.mensajeError.collect { msg ->
                        if (msg != null) {
                            Toast.makeText(this@NewPasswordActivity, msg, Toast.LENGTH_LONG).show()
                            viewModel.limpiarError()
                        }
                    }
                }
                launch {
                    viewModel.resetExitoso.collect { exito ->
                        if (exito) {
                            Toast.makeText(
                                this@NewPasswordActivity,
                                "¡Contraseña actualizada! Iniciá sesión con tu nueva contraseña.",
                                Toast.LENGTH_LONG
                            ).show()
                            val intent = Intent(this@NewPasswordActivity, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            viewModel.limpiarEvento()
                        }
                    }
                }
            }
        }
    }
}
