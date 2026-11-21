package com.example.selaras.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "screening_answers",
    foreignKeys = [
        ForeignKey(
            entity = ScreeningResultEntity::class,
            parentColumns = ["id"],
            childColumns = ["screening_result_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["screening_result_id", "question_number"], unique = true),
        Index(value = ["screening_result_id"])
    ]
)
data class ScreeningAnswerEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "screening_result_id")
    val screeningResultId: Long,

    @ColumnInfo(name = "question_number")
    val questionNumber: Int,

    @ColumnInfo(name = "answer_value")
    val answerValue: Int
)
