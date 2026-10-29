package com.repmomentum.ui

import kotlinx.serialization.Serializable

@Serializable
data object JournalRoute

@Serializable
data object ExercisesRoute

/** Onglet Progression ; sans exercice, affiche le dernier utilisé. */
@Serializable
data class ProgressRoute(val exerciseId: Long? = null)

@Serializable
data object SettingsRoute

/** Saisie d'une série : nouvelle ([setId] nul) ou modification. [epochDay] = jour affiché dans le journal. */
@Serializable
data class LogSetRoute(val setId: Long? = null, val exerciseId: Long? = null, val epochDay: Long? = null)

/** Fiche d'un exercice : progression + actions. */
@Serializable
data class ExerciseDetailRoute(val exerciseId: Long)

@Serializable
data class ExerciseEditRoute(val exerciseId: Long? = null)
