package com.example.finanzas_independientes_app.presentation.common

import android.view.View
import com.example.finanzas_independientes_app.databinding.ViewStateBinding

/**
 * Helper that drives a [ViewStateBinding] include between four mutually
 * exclusive states. The "content" view(s) are managed by the caller.
 *
 * Usage:
 *   val stateHelper = ViewStateHelper(binding.viewState) { viewModel.load() }
 *   stateHelper.showLoading()
 *   stateHelper.showEmpty("No hay elementos")
 *   stateHelper.showError("Error de red") { viewModel.load() }
 *   stateHelper.showContent()   // hides the overlay; caller shows their list
 */
class ViewStateHelper(
    private val stateBinding: ViewStateBinding,
    private val defaultRetry: (() -> Unit)? = null
) {

    fun showLoading() {
        stateBinding.viewStateContainer.visibility = View.VISIBLE
        stateBinding.stateProgressBar.visibility = View.VISIBLE
        stateBinding.stateEmptyIcon.visibility = View.GONE
        stateBinding.stateMessage.visibility = View.GONE
        stateBinding.stateRetryButton.visibility = View.GONE
    }

    fun showEmpty(message: String) {
        stateBinding.viewStateContainer.visibility = View.VISIBLE
        stateBinding.stateProgressBar.visibility = View.GONE
        stateBinding.stateEmptyIcon.visibility = View.VISIBLE
        stateBinding.stateMessage.visibility = View.VISIBLE
        stateBinding.stateMessage.text = message
        stateBinding.stateRetryButton.visibility = View.GONE
    }

    fun showError(message: String, onRetry: (() -> Unit)? = null) {
        stateBinding.viewStateContainer.visibility = View.VISIBLE
        stateBinding.stateProgressBar.visibility = View.GONE
        stateBinding.stateEmptyIcon.visibility = View.GONE
        stateBinding.stateMessage.visibility = View.VISIBLE
        stateBinding.stateMessage.text = message
        stateBinding.stateRetryButton.visibility = View.VISIBLE
        stateBinding.stateRetryButton.setOnClickListener {
            (onRetry ?: defaultRetry)?.invoke()
        }
    }

    fun showContent() {
        stateBinding.viewStateContainer.visibility = View.GONE
    }
}
