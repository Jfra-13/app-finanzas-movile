package com.example.finanzas_independientes_app.presentation.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.finanzas_independientes_app.databinding.ActivityVerificationBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class VerificationActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EMAIL = "extra_email"
    }

    private lateinit var binding: ActivityVerificationBinding
    private val viewModel: VerificationViewModel by lazy {
        ViewModelProvider(this)[VerificationViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val email = intent.getStringExtra(EXTRA_EMAIL) ?: ""

        binding.ivBackArrowOTP.setOnClickListener { finish() }

        binding.btnVerify.setOnClickListener {
            val otp = listOf(
                binding.etOtp1.text.toString(),
                binding.etOtp2.text.toString(),
                binding.etOtp3.text.toString(),
                binding.etOtp4.text.toString()
            ).joinToString("")
            viewModel.verificar(email, otp)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.mensajeError.collect { msg ->
                        if (msg != null) {
                            Toast.makeText(this@VerificationActivity, msg, Toast.LENGTH_LONG).show()
                            viewModel.limpiarError()
                        }
                    }
                }
                launch {
                    viewModel.otpVerificado.collect { pair ->
                        if (pair != null) {
                            val intent = Intent(this@VerificationActivity, NewPasswordActivity::class.java)
                            intent.putExtra(NewPasswordActivity.EXTRA_EMAIL, pair.first)
                            intent.putExtra(NewPasswordActivity.EXTRA_OTP, pair.second)
                            startActivity(intent)
                            viewModel.limpiarEvento()
                        }
                    }
                }
            }
        }
    }
}
