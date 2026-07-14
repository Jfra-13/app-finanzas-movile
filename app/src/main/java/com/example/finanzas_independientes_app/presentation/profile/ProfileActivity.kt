package com.example.finanzas_independientes_app.presentation.profile

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.finanzas_independientes_app.databinding.ActivityProfileBinding
import com.example.finanzas_independientes_app.databinding.LayoutBottomNavigationBinding
import com.example.finanzas_independientes_app.databinding.ViewSettingRowBinding
import com.example.finanzas_independientes_app.core.session.SessionManager
import com.example.finanzas_independientes_app.presentation.categorias.CategoriasActivity
import com.example.finanzas_independientes_app.presentation.common.AddTransactionDialog
import com.example.finanzas_independientes_app.presentation.common.BottomNav
import com.example.finanzas_independientes_app.presentation.common.QuickAddViewModel
import com.example.finanzas_independientes_app.presentation.transacciones.TransaccionesActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val bottomNavBinding: LayoutBottomNavigationBinding by lazy {
        LayoutBottomNavigationBinding.bind(binding.root)
    }
    private val quickAdd: QuickAddViewModel by lazy {
        ViewModelProvider(this)[QuickAddViewModel::class.java]
    }
    private val profileViewModel: ProfileViewModel by lazy {
        ViewModelProvider(this)[ProfileViewModel::class.java]
    }

    @Inject
    lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyStatusBarInset()
        bindHeader()
        bindRows()
        bindNav()

        quickAdd.cargarCategorias()
        lifecycleScope.launch {
            quickAdd.mensajeUI.collect { msg ->
                if (msg != null) {
                    Toast.makeText(this@ProfileActivity, msg, Toast.LENGTH_SHORT).show()
                    quickAdd.limpiarMensaje()
                }
            }
        }
    }

    // Edge-to-edge: keep the photo clear of the status bar.
    private fun applyStatusBarInset() {
        val basePaddingTop = binding.profileScroll.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(binding.profileScroll) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = basePaddingTop + bars.top)
            insets
        }
    }

    // Session values render instantly; the server profile refreshes them when it arrives.
    private fun bindHeader() {
        binding.tvProfileName.text = session.nombre ?: "Usuario"
        binding.tvProfileEmail.text = session.email ?: ""

        lifecycleScope.launch {
            profileViewModel.perfil.collect { perfil ->
                if (perfil != null) {
                    binding.tvProfileName.text = perfil.nombre
                    binding.tvProfileEmail.text = perfil.email
                }
            }
        }
    }

    private fun bindRows() {
        configureRow(binding.rowSuscripcion, "⭐", "Suscripción") { soon() }
        configureRow(binding.rowCuenta, "👤", "Cuenta") {
            startActivity(Intent(this, AccountActivity::class.java))
        }
        configureRow(binding.rowCategorias, "🏷️", "Categorías") {
            startActivity(Intent(this, CategoriasActivity::class.java))
        }
        configureRow(binding.rowMovimientos, "📋", "Movimientos") {
            startActivity(Intent(this, TransaccionesActivity::class.java))
        }
        configureRow(binding.rowApariencia, "🎨", "Apariencia") { soon() }
        configureRow(binding.rowNotificaciones, "🔔", "Notificaciones") { soon() }
        configureRow(binding.rowAyuda, "💬", "Ayuda y comentarios") { soon() }
        configureRow(binding.rowInvitar, "👥", "Invitar a un amigo") { soon() }
    }

    private fun bindNav() {
        BottomNav.setup(this, bottomNavBinding, BottomNav.Tab.PROFILE) {
            AddTransactionDialog.show(this, quickAdd.categorias.value) { monto, tipo, desc, catId ->
                quickAdd.registrarTransaccion(monto, tipo, desc, catId)
            }
        }
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
