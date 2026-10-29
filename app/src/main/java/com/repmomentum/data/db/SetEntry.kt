package com.repmomentum.data.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * Une série, entrée indépendante du journal. Les instants sont en epoch millis (UTC) ;
 * le découpage en jours se fait dans le fuseau local.
 */
@Entity(
    tableName = "set_entries",
    foreignKeys = [
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("exerciseId"), Index("performedAt")],
)
data class SetEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: Long,
    val weight: Double,
    val repetitions: Int,
    val performedAt: Long,
    val notes: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

/** Série accompagnée de son exercice, pour l'affichage du journal. */
data class LoggedSet(
    @Embedded val set: SetEntry,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: Exercise,
)

/** Date de dernière utilisation d'un exercice. */
data class ExerciseUsage(
    val exerciseId: Long,
    @ColumnInfo(name = "lastPerformedAt") val lastPerformedAt: Long,
)
