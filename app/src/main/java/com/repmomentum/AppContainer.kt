package com.repmomentum

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.repmomentum.data.db.RepMomentumDatabase
import com.repmomentum.data.repository.DataStoreSettingsRepository
import com.repmomentum.data.repository.ExerciseRepository
import com.repmomentum.data.repository.RoomExerciseRepository
import com.repmomentum.data.repository.RoomSetRepository
import com.repmomentum.data.repository.SetRepository
import com.repmomentum.data.repository.SettingsRepository
import java.time.Clock

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Dépendances de l'application, créées une seule fois. Remplaçable dans les tests. */
class AppContainer(
    context: Context,
    inMemoryDatabase: Boolean = false,
    val clock: Clock = Clock.systemDefaultZone(),
) {
    val database: RepMomentumDatabase = RepMomentumDatabase.create(context, inMemoryDatabase)
    val exerciseRepository: ExerciseRepository =
        RoomExerciseRepository(database.exerciseDao(), database.setEntryDao())
    val setRepository: SetRepository = RoomSetRepository(database.setEntryDao(), clock)
    val settingsRepository: SettingsRepository = DataStoreSettingsRepository(context.applicationContext.settingsStore)
}
