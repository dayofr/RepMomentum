package com.repmomentum.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.CalendarViewDay
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.repmomentum.R
import com.repmomentum.RepMomentumApplication
import com.repmomentum.ui.exercises.ExerciseEditScreen
import com.repmomentum.ui.exercises.ExercisesScreen
import com.repmomentum.ui.journal.JournalScreen
import com.repmomentum.ui.logset.LogSetScreen
import com.repmomentum.ui.logset.SetSavedEvent
import com.repmomentum.ui.progress.ProgressScreen
import com.repmomentum.ui.settings.SettingsScreen
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

private data class TopLevel(val route: Any, val routeClass: KClass<*>, val label: Int, val icon: ImageVector)

private val topLevels = listOf(
    TopLevel(JournalRoute, JournalRoute::class, R.string.nav_journal, Icons.Outlined.CalendarViewDay),
    TopLevel(ExercisesRoute, ExercisesRoute::class, R.string.nav_exercises, Icons.Outlined.FitnessCenter),
    TopLevel(ProgressRoute(), ProgressRoute::class, R.string.nav_progress, Icons.AutoMirrored.Outlined.ShowChart),
    TopLevel(SettingsRoute, SettingsRoute::class, R.string.nav_settings, Icons.Outlined.Settings),
)

@Composable
fun RepMomentumApp() {
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    val context = LocalContext.current
    val container = (context.applicationContext as RepMomentumApplication).container
    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val showBar = topLevels.any { top -> destination?.hierarchy?.any { it.hasRoute(top.routeClass) } == true }

    /** Confirmation discrète avec annulation possible d'une série ajoutée. */
    fun announce(event: SetSavedEvent) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val message = resources.getString(if (event.isNew) R.string.snackbar_set_saved else R.string.snackbar_set_updated)
            val result = snackbar.showSnackbar(
                message = message,
                actionLabel = if (event.isNew) resources.getString(R.string.action_undo) else null,
                // Plus long quand une annulation est possible : le temps de réagir après l'effort.
                duration = if (event.isNew) SnackbarDuration.Long else SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed && event.isNew) {
                runCatching { container.setRepository.delete(event.setId) }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    topLevels.forEach { top ->
                        val selected = destination?.hierarchy?.any { it.hasRoute(top.routeClass) } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(top.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(top.icon, contentDescription = null) },
                            label = { Text(stringResource(top.label)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        CompositionLocalProvider(LocalSnackbarHostState provides snackbar) {
        NavHost(
            navController = navController,
            startDestination = JournalRoute,
            modifier = Modifier.padding(padding),
        ) {
            composable<JournalRoute> {
                JournalScreen(
                    onAddSet = { epochDay -> navController.navigate(LogSetRoute(epochDay = epochDay)) },
                    onEditSet = { id -> navController.navigate(LogSetRoute(setId = id)) },
                    onSetRepeated = ::announce,
                )
            }
            composable<LogSetRoute> { entry ->
                val route = entry.toRoute<LogSetRoute>()
                LogSetScreen(
                    route = route,
                    onClose = { navController.popBackStack() },
                    onSaved = { event ->
                        navController.popBackStack()
                        announce(event)
                    },
                    onDeleted = {
                        navController.popBackStack()
                        scope.launch { snackbar.showSnackbar(resources.getString(R.string.snackbar_set_deleted)) }
                    },
                )
            }
            composable<ExercisesRoute> {
                ExercisesScreen(
                    onOpenExercise = { id -> navController.navigate(ExerciseDetailRoute(id)) },
                    onLogExercise = { id -> navController.navigate(LogSetRoute(exerciseId = id)) },
                    onCreateExercise = { navController.navigate(ExerciseEditRoute()) },
                )
            }
            composable<ExerciseEditRoute> { entry ->
                ExerciseEditScreen(
                    exerciseId = entry.toRoute<ExerciseEditRoute>().exerciseId,
                    onClose = { navController.popBackStack() },
                    onRemoved = { message ->
                        // L'exercice n'existe plus (ou est archivé) : on revient à la liste.
                        navController.popBackStack(ExercisesRoute, inclusive = false)
                        scope.launch { snackbar.showSnackbar(resources.getString(message)) }
                    },
                )
            }
            composable<ProgressRoute> { entry ->
                ProgressScreen(
                    initialExerciseId = entry.toRoute<ProgressRoute>().exerciseId,
                    isDetail = false,
                    onBack = {},
                    onLogExercise = { id -> navController.navigate(LogSetRoute(exerciseId = id)) },
                    onEditExercise = { id -> navController.navigate(ExerciseEditRoute(id)) },
                    onEditSet = { id -> navController.navigate(LogSetRoute(setId = id)) },
                )
            }
            composable<ExerciseDetailRoute> { entry ->
                ProgressScreen(
                    initialExerciseId = entry.toRoute<ExerciseDetailRoute>().exerciseId,
                    isDetail = true,
                    onBack = { navController.popBackStack() },
                    onLogExercise = { id -> navController.navigate(LogSetRoute(exerciseId = id)) },
                    onEditExercise = { id -> navController.navigate(ExerciseEditRoute(id)) },
                    onEditSet = { id -> navController.navigate(LogSetRoute(setId = id)) },
                )
            }
            composable<SettingsRoute> {
                SettingsScreen()
            }
        }
        }
    }
}

/**
 * Snackbar partagé. Chaque écran l'affiche dans son propre Scaffold :
 * le bouton flottant remonte alors au lieu d'être masqué.
 */
val LocalSnackbarHostState = staticCompositionLocalOf { SnackbarHostState() }

@Composable
fun AppSnackbarHost() = SnackbarHost(LocalSnackbarHostState.current)
