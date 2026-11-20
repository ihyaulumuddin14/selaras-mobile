package com.example.selaras.data.datasource.remote.dto

import java.time.Instant

/**
 * Data Transfer Object for Check-in communication with backend (Node.js + PostgreSQL).
 */
data class CheckInNetworkDto(
    val uuid: String,
    val userId: String? = null,
    val checkInDate: String, // ISO-8601 "YYYY-MM-DD"
    val mood: Int,
    val energy: Int,
    val sleepDurationMinutes: Int,
    val note: String? = null,
    val isDeleted: Boolean = false,
    val createdAt: String, // ISO-8601 Instant string
    val updatedAt: String  // ISO-8601 Instant string
)

/**
 * Request body for batch synchronization push from Android client.
 */
data class SyncCheckInBatchRequestDto(
    val clientTimestamp: String = Instant.now().toString(),
    val items: List<CheckInNetworkDto>
)

/**
 * Server response for batch synchronization push.
 */
data class SyncCheckInBatchResponseDto(
    val serverTimestamp: String = Instant.now().toString(),
    val syncedUuids: List<String> = emptyList(),
    val failedUuids: Map<String, String> = emptyMap()
)
