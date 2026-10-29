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
        ex(
            "Tractions", BACK, BODYWEIGHT, favorite = true,
            "Suspendu à une barre, mains en pronation. Tirer jusqu'au menton au-dessus de la barre, redescendre bras tendus. Lest facultatif.",
        ),
        ex(
            "Pompes", CHEST, BODYWEIGHT, favorite = true,
            "Corps gainé en planche, mains sous les épaules. Descendre la poitrine près du sol, pousser jusqu'aux bras tendus.",
        ),
        ex(
            "Développé couché", CHEST, WEIGHTED, favorite = true,
            "Allongé sur un banc, pieds au sol. Descendre la barre au bas des pectoraux, coudes à 45° environ, puis pousser.",
        ),
        ex(
            "Squat", LEGS, WEIGHTED, favorite = true,
            "Barre sur le haut du dos. Descendre hanches en arrière, genoux dans l'axe des pieds, au moins à la parallèle, puis remonter dos neutre.",
        ),
        ex(
            "Développé incliné", CHEST, WEIGHTED,
            description = "Banc incliné à 30–45°. Descendre la barre ou les haltères au haut des pectoraux, puis pousser.",
        ),
        ex(
            "Dips", TRICEPS, BODYWEIGHT,
            description = "Aux barres parallèles, buste légèrement penché. Descendre jusqu'à 90° aux coudes environ, puis pousser. Lest facultatif.",
        ),
        ex(
            "Curl biceps", BICEPS, WEIGHTED,
            description = "Debout, coudes collés au corps. Fléchir les avant-bras sans balancer le buste, redescendre en contrôlant.",
        ),
        ex(
            "Extension triceps", TRICEPS, WEIGHTED,
            description = "Charge au-dessus de la tête ou à la poulie. Tendre les bras en gardant les coudes fixes.",
        ),
        ex(
            "Soulevé de terre", BACK, WEIGHTED,
            description = "Barre au-dessus du milieu des pieds. Dos neutre, pousser le sol avec les jambes et finir debout hanches tendues.",
        ),
        ex(
            "Rowing", BACK, WEIGHTED,
            description = "Buste penché, dos neutre. Tirer la charge vers le bas du ventre en serrant les omoplates.",
        ),
        ex(
            "Élévations latérales", SHOULDERS, WEIGHTED,
            description = "Haltères le long du corps. Monter les bras sur les côtés jusqu'aux épaules, coudes légèrement fléchis.",
        ),
        ex(
            "Développé militaire", SHOULDERS, WEIGHTED,
            description = "Debout, abdos serrés. Pousser la barre de la clavicule au-dessus de la tête, sans cambrer.",
        ),
        ex(
            "Fentes", LEGS, BODYWEIGHT,
            description = "Grand pas en avant, descendre le genou arrière près du sol, buste droit, puis revenir. Reps par jambe. Lest facultatif.",
        ),
        ex(
            "Squat au poids du corps", LEGS, BODYWEIGHT,
            description = "Pieds largeur d'épaules. Descendre hanches en arrière aussi bas que possible, talons au sol.",
        ),
        ex(
            "Crunchs", CORE, BODYWEIGHT,
            description = "Allongé, genoux fléchis. Enrouler le haut du dos pour décoller les épaules, sans tirer sur la nuque.",
        ),
        ex(
            "Burpees", FULL_BODY, BODYWEIGHT,
            description = "Squat, mains au sol, jambes en arrière en planche, retour, puis saut bras en l'air.",
        ),
        ex(
            "Gainage", CORE, TIMED,
            description = "Planche sur les avant-bras, corps aligné des épaules aux talons. Tenir sans creuser le bas du dos.",
        ),
        ex(
            "Gainage latéral", CORE, TIMED,
            description = "Sur un avant-bras, corps de profil et aligné, hanches hautes. Durée par côté.",
        ),
        ex(
            "Chaise", LEGS, TIMED,
            description = "Dos contre un mur, cuisses parallèles au sol, genoux à 90°. Tenir la position.",
        ),
    )

    /** Ajoutés en version 2 de la base. */
    val addedInV2: List<Exercise> = listOf(
        ex(
            "Kettlebell swing à deux mains", FULL_BODY, WEIGHTED,
            description = "Charnière de hanche : kettlebell entre les jambes, puis extension explosive des hanches jusqu'à hauteur de poitrine. Les hanches propulsent, les bras guident.",
        ),
        ex(
            "Kettlebell halo", SHOULDERS, WEIGHTED,
            description = "Kettlebell tenue par les cornes, tête en bas. La faire tourner autour de la tête, près du visage, abdos serrés. Reps par sens.",
        ),
    )

    /** Ajoutés en version 3 de la base : exercices kettlebell. */
    val addedInV3: List<Exercise> = listOf(
        ex(
            "Goblet squat", LEGS, WEIGHTED,
            description = "Kettlebell tenue contre la poitrine. Descendre entre les genoux, buste droit, puis remonter.",
        ),
        ex(
            "Kettlebell swing à une main", FULL_BODY, WEIGHTED,
            description = "Même mouvement que le swing à deux mains, d'une seule main, sans laisser le buste tourner. Reps par côté.",
        ),
        ex(
            "Kettlebell clean", FULL_BODY, WEIGHTED,
            description = "Depuis le swing, ramener la kettlebell en position rack (contre l'avant-bras, à hauteur d'épaule) sans qu'elle frappe le poignet. Reps par côté.",
        ),
        ex(
            "Clean & press kettlebell", SHOULDERS, WEIGHTED,
            description = "Clean jusqu'en rack, puis développé au-dessus de la tête jusqu'au bras verrouillé. Reps par côté.",
        ),
        ex(
            "Kettlebell snatch", FULL_BODY, WEIGHTED,
            description = "Depuis le swing, monter la kettlebell d'un seul mouvement jusqu'au bras tendu au-dessus de la tête. Technique exigeante. Reps par côté.",
        ),
        ex(
            "Turkish get-up", FULL_BODY, WEIGHTED,
            description = "Allongé, kettlebell bras tendu vers le plafond. Se relever jusqu'à debout étape par étape, regard sur la charge, puis redescendre. Reps par côté.",
        ),
        ex(
            "Développé militaire kettlebell", SHOULDERS, WEIGHTED,
            description = "Depuis la position rack, pousser la kettlebell au-dessus de la tête, abdos et fessiers serrés. Reps par côté.",
        ),
        ex(
            "Rowing kettlebell", BACK, WEIGHTED,
            description = "Une main en appui, dos plat. Tirer la kettlebell vers la hanche, coude près du corps. Reps par côté.",
        ),
        ex(
            "Soulevé de terre kettlebell", BACK, WEIGHTED,
            description = "Kettlebell entre les pieds. Charnière de hanche, dos neutre, se redresser en poussant le sol.",
        ),
        ex(
            "Windmill", CORE, WEIGHTED,
            description = "Kettlebell bras tendu au-dessus de la tête. Pousser la hanche sur le côté et descendre l'autre main vers le pied, regard sur la charge. Reps par côté.",
        ),
        ex(
            "Around the world", CORE, WEIGHTED,
            description = "Debout, passer la kettlebell d'une main à l'autre autour de la taille, bassin stable. Reps par sens.",
        ),
        ex(
            "Farmer walk", FULL_BODY, TIMED,
            description = "Marcher avec une charge lourde dans chaque main, épaules basses, buste droit. Durée en secondes ; noter la charge en note.",
        ),
    )

    val exercises: List<Exercise> = initial + addedInV2 + addedInV3

    private fun ex(
        name: String,
        group: MuscleGroup,
        type: ExerciseType,
        favorite: Boolean = false,
        description: String,
    ) = Exercise(
        name = name,
        muscleGroup = group,
        exerciseType = type,
        loadUnit = if (type == TIMED) LoadUnit.NONE else LoadUnit.KG,
        isFavorite = favorite,
        description = description,
    )
}
