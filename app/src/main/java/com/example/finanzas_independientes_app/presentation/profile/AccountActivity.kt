package com.example.finanzas_independientes_app.presentation.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.finanzas_independientes_app.core.session.SessionManager
import com.example.finanzas_independientes_app.databinding.ActivityAccountBinding
import com.example.finanzas_independientes_app.databinding.ViewSettingRowBinding
import com.example.finanzas_independientes_app.presentation.auth.LoginActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Account settings. Only what the backend actually supports is wired: viewing the
 * email (from the session) and logging out. Reset-stats, delete-account and
 * change-phone have no endpoint yet, so they announce themselves as upcoming
 * rather than pretending to work.
 */
@AndroidEntryPoint
class AccountActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccountBinding

    @Inject
    lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        configureRow(binding.rowEmail, "✉️", "Correo electrónico") { showEmailDetail() }
        configureRow(binding.rowTelefono, "📱", "Cambiar número de teléfono") { soon() }
        configureRow(binding.rowReset, "🔄", "Resetear estadísticas mensuales") { soon() }
        configureRow(binding.rowEliminar, "🗑️", "Eliminar cuenta") { soon() }
        configureRow(binding.rowLogout, "🚪", "Cerrar sesión") { confirmLogout() }
    }

    // Reveal the detail panel with the account email (icon + label + value).
    private fun showEmailDetail() {
        binding.detailIcon.text = "✉️"
        binding.detailLabel.text = "Correo electrónico"
        binding.detailValue.text = session.email ?: "No disponible"
        binding.detailCard.visibility = View.VISIBLE
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Querés cerrar tu sesión?")
            .setPositiveButton("Cerrar sesión") { _, _ -> logout() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun logout() {
        session.clear()
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
