package com.example.selaras.data.datasource.remote.dto

import java.time.Instant

data class ScreeningAnswerNetworkDto(
    val questionNumber: Int,
    val answerValue: Int
)

data class ScreeningNetworkDto(
    val uuid: String,
    val screeningType: String, // "MANUAL" or "ROUTINE"
    val periodStartDate: String, // "YYYY-MM-DD"
    val periodEndDate: String,   // "YYYY-MM-DD"
    val instrumentVersion: String = "PHQ-9",
    val totalScore: Int,
    val category: String, // "MINIMAL", "RINGAN", "SEDANG", "SEDANG_BERAT", "BERAT"
    val helpIndicator: Boolean,
    val completedAt: String, // ISO-8601
    val isDeleted: Boolean = false,
    val createdAt: String,   // ISO-8601
    val updatedAt: String,   // ISO-8601
    val answers: List<ScreeningAnswerNetworkDto>
)

data class SyncScreeningBatchRequestDto(
    val clientTimestamp: String = Instant.now().toString(),
    val items: List<ScreeningNetworkDto>
)

data class SyncScreeningBatchResponseDto(
    val serverTimestamp: String = Instant.now().toString(),
    val syncedUuids: List<String> = emptyList(),
    val failedUuids: Map<String, String> = emptyMap()
)
