package com.example.finanzas_independientes_app.presentation.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import com.example.finanzas_independientes_app.databinding.ActivityForgotPasswordBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private val viewModel: ForgotPasswordViewModel by lazy {
        ViewModelProvider(this)[ForgotPasswordViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivBackArrowForgot.setOnClickListener { finish() }

        binding.btnSendResetCode.setOnClickListener {
            val email = binding.etForgotEmail.text.toString()
            viewModel.enviarOtp(email)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.mensajeError.collect { msg ->
                        if (msg != null) {
                            Toast.makeText(this@ForgotPasswordActivity, msg, Toast.LENGTH_SHORT).show()
                            viewModel.limpiarError()
                        }
                    }
                }
                launch {
                    viewModel.otpEnviado.collect { email ->
                        if (email != null) {
                            val intent = Intent(this@ForgotPasswordActivity, VerificationActivity::class.java)
                            intent.putExtra(VerificationActivity.EXTRA_EMAIL, email)
                            startActivity(intent)
                            viewModel.limpiarEvento()
                        }
                    }
                }
            }
        }
    }
}
