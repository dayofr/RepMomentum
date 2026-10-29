package com.repmomentum.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.repmomentum.data.db.LoadUnit
import com.repmomentum.data.repository.SettingsRepository
import com.repmomentum.data.repository.UserSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    val settings: StateFlow<UserSettings> =
        repository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    fun setIncrement(value: Double) {
        viewModelScope.launch { runCatching { repository.setWeightIncrement(value) } }
    }

    fun setDefaultUnit(unit: LoadUnit) {
        viewModelScope.launch { runCatching { repository.setDefaultUnit(unit) } }
    }
}
