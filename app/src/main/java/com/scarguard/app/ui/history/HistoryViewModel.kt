package com.scarguard.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scarguard.app.ScarGuardApp
import com.scarguard.app.data.Reading
import com.scarguard.app.data.ScarProfile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class HistoryUiState(
    val profile: ScarProfile? = null,
    val readings: List<Reading> = emptyList(),
)

class HistoryViewModel(app: ScarGuardApp) : ViewModel() {
    private val repository = app.monitoringRepository

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val readings = repository.activeProfile.flatMapLatest { profile ->
        if (profile == null) flowOf(emptyList<Reading>()) else repository.readingsForProfile(profile.id)
    }

    val uiState: StateFlow<HistoryUiState> = combine(repository.activeProfile, readings) { profile, list ->
        HistoryUiState(profile = profile, readings = list)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryUiState())
}
