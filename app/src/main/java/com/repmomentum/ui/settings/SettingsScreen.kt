package com.repmomentum.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.repmomentum.BuildConfig
import com.repmomentum.R
import com.repmomentum.ui.AppSnackbarHost
import com.repmomentum.data.db.LoadUnit
import com.repmomentum.data.repository.UserSettings
import com.repmomentum.domain.formatDecimal
import com.repmomentum.ui.common.PanelCard
import com.repmomentum.ui.common.appViewModelFactory
import com.repmomentum.ui.common.unitLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(factory = appViewModelFactory { c, _ -> SettingsViewModel(c.settingsRepository) }),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    Scaffold(
        snackbarHost = { AppSnackbarHost() },
        contentWindowInsets = WindowInsets(0),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_settings)) }) },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SettingCard(
                title = stringResource(R.string.settings_increment),
                description = stringResource(R.string.settings_increment_help),
            ) {
                val options = UserSettings.INCREMENTS
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    options.forEachIndexed { index, value ->
                        SegmentedButton(
                            selected = settings.weightIncrement == value,
                            onClick = { viewModel.setIncrement(value) },
                            shape = SegmentedButtonDefaults.itemShape(index, options.size),
                            label = { Text(formatDecimal(value)) },
                        )
                    }
                }
            }
            SettingCard(
                title = stringResource(R.string.settings_unit),
                description = stringResource(R.string.settings_unit_help),
            ) {
                val units = listOf(LoadUnit.KG, LoadUnit.LB)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    units.forEachIndexed { index, unit ->
                        SegmentedButton(
                            selected = settings.defaultUnit == unit,
                            onClick = { viewModel.setDefaultUnit(unit) },
                            shape = SegmentedButtonDefaults.itemShape(index, units.size),
                            label = { Text(unitLabel(unit)) },
                        )
                    }
                }
            }
            SettingCard(
                title = stringResource(R.string.settings_data),
                description = stringResource(R.string.settings_data_help),
            ) {}
            Text(
                stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingCard(title: String, description: String, content: @Composable () -> Unit) {
    PanelCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}
