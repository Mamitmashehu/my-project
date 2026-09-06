package com.scarguard.app.ui.monitor

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scarguard.app.ScarGuardApp
import com.scarguard.app.data.ScarProfile
import com.scarguard.app.repository.MonitoringRepository
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MonitorViewModel(app: ScarGuardApp) : ViewModel() {
    private val repository = app.monitoringRepository

    val activeProfile: StateFlow<ScarProfile?> =
        repository.activeProfile.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _liveTemperature = MutableStateFlow<Float?>(null)
    val liveTemperature: StateFlow<Float?> = _liveTemperature.asStateFlow()

    init {
        viewModelScope.launch {
            repository.bleManager.temperatureUpdates.collect { _liveTemperature.value = it }
        }
    }

    fun analyzeAndSave(
        file: File,
        bitmap: Bitmap,
        onResult: (MonitoringRepository.PhotoReadingOutcome) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                val profile = repository.activeProfile.first()
                    ?: throw IllegalStateException("Set a baseline photo first")
                val outcome = repository.recordPhotoReading(profile, file, bitmap, _liveTemperature.value)
                onResult(outcome)
            } catch (t: Throwable) {
                onError(t)
            }
        }
    }
}
