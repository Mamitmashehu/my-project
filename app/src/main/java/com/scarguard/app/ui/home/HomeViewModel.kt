package com.scarguard.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scarguard.app.ScarGuardApp
import com.scarguard.app.ble.ConnectionState
import com.scarguard.app.data.Reading
import com.scarguard.app.data.RiskClassifier
import com.scarguard.app.data.RiskLevel
import com.scarguard.app.data.ScarProfile
import com.scarguard.app.settings.AlertThresholds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val connectionState: ConnectionState = ConnectionState.Idle,
    val liveTemperature: Float? = null,
    val profile: ScarProfile? = null,
    val latestReading: Reading? = null,
    val chartPoints: List<Pair<Long, Float>> = emptyList(),
    val liveTempDelta: Float? = null,
    val liveRisk: RiskLevel = RiskLevel.NORMAL,
    val thresholds: AlertThresholds = AlertThresholds(),
)

class HomeViewModel(app: ScarGuardApp) : ViewModel() {
    private val repository = app.monitoringRepository
    private val ble = repository.bleManager

    private val liveTemperature = MutableStateFlow<Float?>(null)

    init {
        viewModelScope.launch {
            ble.temperatureUpdates.collect { liveTemperature.value = it }
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val readingsForActiveProfile = repository.activeProfile.flatMapLatest { profile ->
        if (profile == null) flowOf(emptyList<Reading>()) else repository.readingsForProfile(profile.id)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        ble.connectionState,
        liveTemperature,
        repository.activeProfile,
        readingsForActiveProfile,
        repository.settingsRepository.thresholds,
    ) { connectionState, liveTemp, profile, readings, thresholds ->
        val delta = if (liveTemp != null && profile?.baselineTemperatureC != null) {
            liveTemp - profile.baselineTemperatureC
        } else null
        val latestReading = readings.firstOrNull()
        val liveRisk = RiskClassifier.classify(delta, latestReading?.rednessDelta, thresholds)
        val chartPoints = readings
            .filter { it.temperatureC != null }
            .sortedBy { it.timestamp }
            .takeLast(30)
            .map { it.timestamp to (it.temperatureC ?: 0f) }

        HomeUiState(
            connectionState = connectionState,
            liveTemperature = liveTemp,
            profile = profile,
            latestReading = latestReading,
            chartPoints = chartPoints,
            liveTempDelta = delta,
            liveRisk = liveRisk,
            thresholds = thresholds,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())
}
