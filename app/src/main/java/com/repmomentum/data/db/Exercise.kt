package com.repmomentum.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Groupe musculaire principal, utilisé pour filtrer la bibliothèque. */
enum class MuscleGroup { CHEST, BACK, SHOULDERS, BICEPS, TRICEPS, LEGS, CORE, FULL_BODY }

/**
 * Nature de l'effort, qui fixe le sens de [SetEntry.weight] et [SetEntry.repetitions] :
 * - [WEIGHTED] : charge externe, poids > 0, volume = poids × reps.
 * - [BODYWEIGHT] : poids du corps, poids = lest ajouté (0 = sans lest). Pas de volume en kg.
 * - [TIMED] : effort tenu (gainage), repetitions = secondes, poids ignoré.
 */
enum class ExerciseType { WEIGHTED, BODYWEIGHT, TIMED }

/** Unité de charge. [NONE] pour les exercices sans charge (durée). */
enum class LoadUnit { KG, LB, NONE }

@Entity(
    tableName = "exercises",
    indices = [Index("name")],
)
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val muscleGroup: MuscleGroup,
    val exerciseType: ExerciseType,
    val loadUnit: LoadUnit,
    val isFavorite: Boolean = false,
    val isCustom: Boolean = false,
    /** Exercice perso retiré de la bibliothèque mais conservé pour l'historique. */
    val isArchived: Boolean = false,
)
