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
import com.example.finanzas_independientes_app.databinding.DialogDeleteAccountBinding
import com.example.finanzas_independientes_app.databinding.DialogInputTextBinding
import com.example.finanzas_independientes_app.databinding.ViewSettingRowBinding
import com.google.android.material.textfield.TextInputLayout
import com.example.finanzas_independientes_app.presentation.auth.LoginActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Account settings. Wires what the backend supports: viewing the email, changing
 * the phone number (PUT /usuarios/me), deleting the account (soft-delete with a
 * 30-day grace) and logging out with server-side refresh-token revocation.
 * Reset-stats has no endpoint yet, so it announces itself as upcoming.
 */
@AndroidEntryPoint
class AccountActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccountBinding
    private val viewModel: AccountViewModel by lazy {
        ViewModelProvider(this)[AccountViewModel::class.java]
    }

    // Set while the delete dialog is open so inline errors land on its field.
    private var deletePasswordTil: TextInputLayout? = null

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
        configureRow(binding.rowEliminar, "🗑️", "Eliminar cuenta") { showDeleteAccountDialog() }
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
        lifecycleScope.launch {
            viewModel.deleteAccountError.collect { error ->
                if (error != null) {
                    val til = deletePasswordTil
                    if (til != null) {
                        til.error = error
                    } else {
                        Toast.makeText(this@AccountActivity, error, Toast.LENGTH_SHORT).show()
                    }
                    viewModel.limpiarDeleteError()
                }
            }
        }
    }

    // Danger zone: soft-delete with a 30-day grace, gated by the account password.
    // Wrong password shows an inline error; the dialog stays open until success or cancel.
    private fun showDeleteAccountDialog() {
        val dialogBinding = DialogDeleteAccountBinding.inflate(LayoutInflater.from(this))
        deletePasswordTil = dialogBinding.tilPassword

        val dialog = AlertDialog.Builder(this)
            .setTitle("Eliminar cuenta")
            .setMessage(
                "Tu cuenta se eliminará en 30 días. Iniciá sesión antes de ese plazo " +
                    "para recuperarla con todos tus datos.\n\nConfirmá con tu contraseña."
            )
            .setView(dialogBinding.root)
            .setPositiveButton("Eliminar", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnDismissListener { deletePasswordTil = null }
        dialog.show()

        // Override after show() so a wrong password does not auto-dismiss the dialog.
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            dialogBinding.tilPassword.error = null
            viewModel.eliminarCuenta(dialogBinding.etPassword.text?.toString() ?: "")
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
