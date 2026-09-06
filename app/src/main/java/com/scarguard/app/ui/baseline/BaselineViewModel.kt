package com.scarguard.app.ui.baseline

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scarguard.app.ScarGuardApp
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BaselineViewModel(app: ScarGuardApp) : ViewModel() {
    private val repository = app.monitoringRepository

    private val _liveTemperature = MutableStateFlow<Float?>(null)
    val liveTemperature: StateFlow<Float?> = _liveTemperature.asStateFlow()

    init {
        viewModelScope.launch {
            repository.bleManager.temperatureUpdates.collect { _liveTemperature.value = it }
        }
    }

    fun saveBaseline(label: String, file: File, bitmap: Bitmap, currentTemperatureC: Float?, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.createBaseline(
                label = label.ifBlank { "Incision" },
                photoFile = file,
                photoBitmap = bitmap,
                currentTemperatureC = currentTemperatureC,
            )
            onDone()
        }
    }
}
