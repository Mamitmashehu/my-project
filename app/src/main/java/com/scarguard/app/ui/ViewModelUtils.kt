package com.scarguard.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.scarguard.app.ScarGuardApp

/**
 * The app has no DI framework: every ViewModel just needs the single [ScarGuardApp] composition
 * root. This helper wires that up without boilerplate at each call site.
 */
@Composable
inline fun <reified VM : ViewModel> scarGuardViewModel(crossinline create: (ScarGuardApp) -> VM): VM {
    val app = LocalContext.current.applicationContext as ScarGuardApp
    val factory = viewModelFactory {
        initializer { create(app) }
    }
    return viewModel(factory = factory)
}
