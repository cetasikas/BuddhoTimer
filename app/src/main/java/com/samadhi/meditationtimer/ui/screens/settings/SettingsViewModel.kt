package com.samadhi.meditationtimer.ui.screens.settings

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.PowerManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.samadhi.meditationtimer.MeditationApp
import com.samadhi.meditationtimer.data.backup.BackupManager
import com.samadhi.meditationtimer.domain.repository.SessionRepository
import com.samadhi.meditationtimer.service.DndManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val repository: SessionRepository = (application as MeditationApp).sessionRepository
    private val backupManager = BackupManager(context, repository)
    private val dndManager = DndManager(context)

    private val _isDndGranted = MutableStateFlow(dndManager.isDndAccessGranted)
    val isDndGranted: StateFlow<Boolean> = _isDndGranted.asStateFlow()

    private val _isBatteryOptIgnored = MutableStateFlow(checkBatteryOptimization())
    val isBatteryOptIgnored: StateFlow<Boolean> = _isBatteryOptIgnored.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun refreshPermissions() {
        _isDndGranted.value = dndManager.isDndAccessGranted
        _isBatteryOptIgnored.value = checkBatteryOptimization()
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun exportJsonBackup(uri: Uri) {
        viewModelScope.launch {
            val result = backupManager.exportToJson(uri)
            result.onSuccess { count ->
                _statusMessage.value = "Exported $count sessions to JSON backup"
            }.onFailure { error ->
                _statusMessage.value = "Export failed: ${error.localizedMessage}"
            }
        }
    }

    fun exportCsvBackup(uri: Uri) {
        viewModelScope.launch {
            val result = backupManager.exportToCsv(uri)
            result.onSuccess { count ->
                _statusMessage.value = "Exported $count sessions to CSV"
            }.onFailure { error ->
                _statusMessage.value = "Export failed: ${error.localizedMessage}"
            }
        }
    }

    fun importJsonBackup(uri: Uri) {
        viewModelScope.launch {
            val result = backupManager.importFromJson(uri, overwrite = false)
            result.onSuccess { count ->
                _statusMessage.value = "Successfully imported $count sessions"
            }.onFailure { error ->
                _statusMessage.value = "Import failed: ${error.localizedMessage}"
            }
        }
    }

    private fun checkBatteryOptimization(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true
    }
}
