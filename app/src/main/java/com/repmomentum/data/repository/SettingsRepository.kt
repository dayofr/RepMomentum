package com.repmomentum.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.repmomentum.data.db.LoadUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

data class UserSettings(
    val weightIncrement: Double = DEFAULT_INCREMENT,
    val defaultUnit: LoadUnit = LoadUnit.KG,
) {
    companion object {
        const val DEFAULT_INCREMENT = 2.5
        val INCREMENTS = listOf(0.5, 1.0, 1.25, 2.5, 5.0)
    }
}

interface SettingsRepository {
    val settings: Flow<UserSettings>
    suspend fun setWeightIncrement(value: Double)
    suspend fun setDefaultUnit(unit: LoadUnit)
}

class DataStoreSettingsRepository(private val store: DataStore<Preferences>) : SettingsRepository {
    private val incrementKey = doublePreferencesKey("weight_increment")
    private val unitKey = stringPreferencesKey("default_unit")

    override val settings: Flow<UserSettings> = store.data
        .catch { error -> if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw error }
        .map { prefs ->
            UserSettings(
                weightIncrement = prefs[incrementKey] ?: UserSettings.DEFAULT_INCREMENT,
                defaultUnit = prefs[unitKey]
                    ?.let { name -> LoadUnit.entries.firstOrNull { it.name == name } }
                    ?.takeIf { it != LoadUnit.NONE }
                    ?: LoadUnit.KG,
            )
        }

    override suspend fun setWeightIncrement(value: Double) {
        store.edit { it[incrementKey] = value }
    }

    override suspend fun setDefaultUnit(unit: LoadUnit) {
        require(unit != LoadUnit.NONE)
        store.edit { it[unitKey] = unit.name }
    }
}
