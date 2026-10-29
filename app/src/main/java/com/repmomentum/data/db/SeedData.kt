package com.repmomentum.data.db

import com.repmomentum.data.db.ExerciseType.BODYWEIGHT
import com.repmomentum.data.db.ExerciseType.TIMED
import com.repmomentum.data.db.ExerciseType.WEIGHTED
import com.repmomentum.data.db.MuscleGroup.BACK
import com.repmomentum.data.db.MuscleGroup.BICEPS
import com.repmomentum.data.db.MuscleGroup.CHEST
import com.repmomentum.data.db.MuscleGroup.CORE
import com.repmomentum.data.db.MuscleGroup.FULL_BODY
import com.repmomentum.data.db.MuscleGroup.LEGS
import com.repmomentum.data.db.MuscleGroup.SHOULDERS
import com.repmomentum.data.db.MuscleGroup.TRICEPS

/**
 * Exercices préchargés. Quelques favoris pour un premier ajout immédiat.
 * Un ajout à la liste doit aussi passer par une migration pour les bases existantes.
 */
object SeedData {
    private val initial: List<Exercise> = listOf(
        ex("Tractions", BACK, BODYWEIGHT, favorite = true),
        ex("Pompes", CHEST, BODYWEIGHT, favorite = true),
        ex("Développé couché", CHEST, WEIGHTED, favorite = true),
        ex("Squat", LEGS, WEIGHTED, favorite = true),
        ex("Développé incliné", CHEST, WEIGHTED),
        ex("Dips", TRICEPS, BODYWEIGHT),
        ex("Curl biceps", BICEPS, WEIGHTED),
        ex("Extension triceps", TRICEPS, WEIGHTED),
        ex("Soulevé de terre", BACK, WEIGHTED),
        ex("Rowing", BACK, WEIGHTED),
        ex("Élévations latérales", SHOULDERS, WEIGHTED),
        ex("Développé militaire", SHOULDERS, WEIGHTED),
        ex("Fentes", LEGS, BODYWEIGHT),
        ex("Squat au poids du corps", LEGS, BODYWEIGHT),
        ex("Crunchs", CORE, BODYWEIGHT),
        ex("Burpees", FULL_BODY, BODYWEIGHT),
        ex("Gainage", CORE, TIMED),
        ex("Gainage latéral", CORE, TIMED),
        ex("Chaise", LEGS, TIMED),
    )

    /** Ajoutés en version 2 de la base. */
    val addedInV2: List<Exercise> = listOf(
        ex("Kettlebell swing à deux mains", FULL_BODY, WEIGHTED),
        ex("Kettlebell halo", SHOULDERS, WEIGHTED),
    )

    val exercises: List<Exercise> = initial + addedInV2

    private fun ex(name: String, group: MuscleGroup, type: ExerciseType, favorite: Boolean = false) = Exercise(
        name = name,
        muscleGroup = group,
        exerciseType = type,
        loadUnit = if (type == TIMED) LoadUnit.NONE else LoadUnit.KG,
        isFavorite = favorite,
    )
}
