package com.example.selaras.data.datasource.remote.dto

import java.time.Instant

/**
 * Data Transfer Object for Custom Breathing Pattern sync with backend.
 */
data class BreathingPatternNetworkDto(
    val uuid: String,
    val userId: String? = null,
    val name: String,
    val inhaleDurationSeconds: Int,
    val exhaleDurationSeconds: Int,
    val isDeleted: Boolean = false,
    val createdAt: String, // ISO-8601 Instant string
    val updatedAt: String  // ISO-8601 Instant string
)

/**
 * Request body for batch synchronization push of custom breathing patterns.
 */
data class SyncBreathingBatchRequestDto(
    val clientTimestamp: String = Instant.now().toString(),
    val items: List<BreathingPatternNetworkDto>
)

/**
 * Server response for batch synchronization push of custom breathing patterns.
 */
data class SyncBreathingBatchResponseDto(
    val serverTimestamp: String = Instant.now().toString(),
    val syncedUuids: List<String> = emptyList(),
    val failedUuids: Map<String, String> = emptyMap()
)
