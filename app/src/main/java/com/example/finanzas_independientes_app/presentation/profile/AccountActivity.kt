package com.example.finanzas_independientes_app.presentation.profile

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.finanzas_independientes_app.core.session.SessionManager
import com.example.finanzas_independientes_app.databinding.ActivityAccountBinding
import com.example.finanzas_independientes_app.databinding.DialogInputTextBinding
import com.example.finanzas_independientes_app.databinding.ViewSettingRowBinding
import com.example.finanzas_independientes_app.presentation.auth.LoginActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Account settings. Only what the backend actually supports is wired: viewing the
 * email, changing the phone number (PUT /usuarios/me) and logging out with
 * server-side refresh-token revocation. Reset-stats and delete-account have no
 * endpoint yet, so they announce themselves as upcoming rather than pretending to work.
 */
@AndroidEntryPoint
class AccountActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccountBinding
    private val viewModel: AccountViewModel by lazy {
        ViewModelProvider(this)[AccountViewModel::class.java]
    }

    @Inject
    lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        configureRow(binding.rowEmail, "✉️", "Correo electrónico") { showEmailDetail() }
        configureRow(binding.rowNombre, "👤", "Cambiar nombre") { showNameDialog() }
        configureRow(binding.rowTelefono, "📱", "Cambiar número de teléfono") { showPhoneDialog() }
        configureRow(binding.rowReset, "🔄", "Resetear estadísticas mensuales") { soon() }
        configureRow(binding.rowEliminar, "🗑️", "Eliminar cuenta") { soon() }
        configureRow(binding.rowLogout, "🚪", "Cerrar sesión") { confirmLogout() }

        bindFlows()
    }

    private fun bindFlows() {
        lifecycleScope.launch {
            viewModel.logoutDone.collect { done ->
                if (done) goToLogin()
            }
        }
        lifecycleScope.launch {
            viewModel.mensaje.collect { msg ->
                if (msg != null) {
                    Toast.makeText(this@AccountActivity, msg, Toast.LENGTH_SHORT).show()
                    viewModel.limpiarMensaje()
                }
            }
        }
    }

    // Reveal the detail panel with the account email (icon + label + value).
    private fun showEmailDetail() {
        binding.detailIcon.text = "✉️"
        binding.detailLabel.text = "Correo electrónico"
        binding.detailValue.text = viewModel.perfil.value?.email ?: session.email ?: "No disponible"
        binding.detailCard.visibility = View.VISIBLE
    }

    private fun showNameDialog() {
        val dialogBinding = DialogInputTextBinding.inflate(LayoutInflater.from(this))
        dialogBinding.tvDialogTitle.text = "Cambiar nombre"
        dialogBinding.tilInput.hint = "Nombre"
        dialogBinding.etInput.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        dialogBinding.etInput.setText(viewModel.perfil.value?.nombre ?: session.nombre ?: "")

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancelar.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnGuardar.setOnClickListener {
            viewModel.cambiarNombre(dialogBinding.etInput.text?.toString() ?: "")
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showPhoneDialog() {
        val dialogBinding = DialogInputTextBinding.inflate(LayoutInflater.from(this))
        dialogBinding.tvDialogTitle.text = "Cambiar número de teléfono"
        dialogBinding.tilInput.hint = "Teléfono"
        dialogBinding.etInput.inputType = InputType.TYPE_CLASS_PHONE
        dialogBinding.etInput.setText(viewModel.perfil.value?.telefono ?: "")

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancelar.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnGuardar.setOnClickListener {
            viewModel.cambiarTelefono(dialogBinding.etInput.text?.toString() ?: "")
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Querés cerrar tu sesión?")
            .setPositiveButton("Cerrar sesión") { _, _ -> viewModel.logout() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        finishAffinity()
    }

    private fun configureRow(
        row: ViewSettingRowBinding,
        icon: String,
        label: String,
        onClick: () -> Unit
    ) {
        row.rowIcon.text = icon
        row.rowLabel.text = label
        row.root.setOnClickListener { onClick() }
    }

    private fun soon() {
        Toast.makeText(this, "Próximamente", Toast.LENGTH_SHORT).show()
    }
}
