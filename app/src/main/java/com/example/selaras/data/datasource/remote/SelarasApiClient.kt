package com.example.selaras.data.datasource.remote

import com.example.selaras.data.datasource.remote.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

/**
 * Standard HTTP REST API Client communicating with the Node.js/Express Selaras Backend.
 * Uses native java.net.HttpURLConnection and org.json for zero external dependencies.
 */
class SelarasApiClient(
    private val baseUrl: String = DEFAULT_BASE_URL
) {

    suspend fun register(
        request: RegisterRequestDto
    ): Result<AuthResponseDto> = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("email", request.email)
            put("password", request.password)
            if (!request.userName.isNullOrBlank()) {
                put("userName", request.userName)
            }
        }

        executeRequest(
            endpoint = "/auth/register",
            method = "POST",
            body = payload.toString(),
            token = null
        ).mapCatching { json ->
            val data = json.getJSONObject("data")
            parseAuthResponse(data)
        }
    }

    suspend fun login(
        request: LoginRequestDto
    ): Result<AuthResponseDto> = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("email", request.email)
            put("password", request.password)
        }

        executeRequest(
            endpoint = "/auth/login",
            method = "POST",
            body = payload.toString(),
            token = null
        ).mapCatching { json ->
            val data = json.getJSONObject("data")
            parseAuthResponse(data)
        }
    }

    suspend fun pushCheckIns(
        token: String,
        request: SyncCheckInBatchRequestDto
    ): Result<SyncCheckInBatchResponseDto> = withContext(Dispatchers.IO) {
        val itemsArray = JSONArray()
        for (item in request.items) {
            val obj = JSONObject().apply {
                put("uuid", item.uuid)
                put("checkInDate", item.checkInDate)
                put("mood", item.mood)
                put("energy", item.energy)
                put("sleepDurationMinutes", item.sleepDurationMinutes)
                put("note", item.note ?: JSONObject.NULL)
                put("isDeleted", item.isDeleted)
                put("createdAt", item.createdAt)
                put("updatedAt", item.updatedAt)
            }
            itemsArray.put(obj)
        }

        val payload = JSONObject().apply {
            put("clientTimestamp", request.clientTimestamp)
            put("items", itemsArray)
        }

        executeRequest(
            endpoint = "/sync/check-ins/push",
            method = "POST",
            body = payload.toString(),
            token = token
        ).mapCatching { json ->
            val data = json.getJSONObject("data")
            val syncedArray = data.getJSONArray("syncedUuids")
            val syncedList = mutableListOf<String>()
            for (i in 0 until syncedArray.length()) {
                syncedList.add(syncedArray.getString(i))
            }
            SyncCheckInBatchResponseDto(
                serverTimestamp = data.optString("serverTimestamp", Instant.now().toString()),
                syncedUuids = syncedList
            )
        }
    }

    suspend fun pullCheckIns(
        token: String,
        since: Instant
    ): Result<List<CheckInNetworkDto>> = withContext(Dispatchers.IO) {
        val query = if (since != Instant.EPOCH) "?since=${since}" else ""
        executeRequest(
            endpoint = "/sync/check-ins/pull$query",
            method = "GET",
            body = null,
            token = token
        ).mapCatching { json ->
            val data = json.getJSONObject("data")
            val itemsArray = data.getJSONArray("items")
            val list = mutableListOf<CheckInNetworkDto>()
            for (i in 0 until itemsArray.length()) {
                val item = itemsArray.getJSONObject(i)
                list.add(
                    CheckInNetworkDto(
                        uuid = item.getString("uuid"),
                        checkInDate = item.getString("checkInDate"),
                        mood = item.getInt("mood"),
                        energy = item.getInt("energy"),
                        sleepDurationMinutes = item.getInt("sleepDurationMinutes"),
                        note = if (item.isNull("note")) null else item.optString("note"),
                        isDeleted = item.optBoolean("isDeleted", false),
                        createdAt = item.getString("createdAt"),
                        updatedAt = item.getString("updatedAt")
                    )
                )
            }
            list
        }
    }

    suspend fun pushBreathingPatterns(
        token: String,
        request: SyncBreathingBatchRequestDto
    ): Result<SyncBreathingBatchResponseDto> = withContext(Dispatchers.IO) {
        val itemsArray = JSONArray()
        for (item in request.items) {
            val obj = JSONObject().apply {
                put("uuid", item.uuid)
                put("name", item.name)
                put("inhaleDurationSeconds", item.inhaleDurationSeconds)
                put("exhaleDurationSeconds", item.exhaleDurationSeconds)
                put("isDeleted", item.isDeleted)
                put("createdAt", item.createdAt)
                put("updatedAt", item.updatedAt)
            }
            itemsArray.put(obj)
        }

        val payload = JSONObject().apply {
            put("clientTimestamp", request.clientTimestamp)
            put("items", itemsArray)
        }

        executeRequest(
            endpoint = "/sync/breathing-patterns/push",
            method = "POST",
            body = payload.toString(),
            token = token
        ).mapCatching { json ->
            val data = json.getJSONObject("data")
            val syncedArray = data.getJSONArray("syncedUuids")
            val syncedList = mutableListOf<String>()
            for (i in 0 until syncedArray.length()) {
                syncedList.add(syncedArray.getString(i))
            }
            SyncBreathingBatchResponseDto(
                serverTimestamp = data.optString("serverTimestamp", Instant.now().toString()),
                syncedUuids = syncedList
            )
        }
    }

    suspend fun pullBreathingPatterns(
        token: String,
        since: Instant
    ): Result<List<BreathingPatternNetworkDto>> = withContext(Dispatchers.IO) {
        val query = if (since != Instant.EPOCH) "?since=${since}" else ""
        executeRequest(
            endpoint = "/sync/breathing-patterns/pull$query",
            method = "GET",
            body = null,
            token = token
        ).mapCatching { json ->
            val data = json.getJSONObject("data")
            val itemsArray = data.getJSONArray("items")
            val list = mutableListOf<BreathingPatternNetworkDto>()
            for (i in 0 until itemsArray.length()) {
                val item = itemsArray.getJSONObject(i)
                list.add(
                    BreathingPatternNetworkDto(
                        uuid = item.getString("uuid"),
                        name = item.getString("name"),
                        inhaleDurationSeconds = item.getInt("inhaleDurationSeconds"),
                        exhaleDurationSeconds = item.getInt("exhaleDurationSeconds"),
                        isDeleted = item.optBoolean("isDeleted", false),
                        createdAt = item.getString("createdAt"),
                        updatedAt = item.getString("updatedAt")
                    )
                )
            }
            list
        }
    }

    suspend fun pushScreenings(
        token: String,
        request: SyncScreeningBatchRequestDto
    ): Result<SyncScreeningBatchResponseDto> = withContext(Dispatchers.IO) {
        val itemsArray = JSONArray()
        for (item in request.items) {
            val answersArray = JSONArray()
            for (a in item.answers) {
                answersArray.put(
                    JSONObject().apply {
                        put("questionNumber", a.questionNumber)
                        put("answerValue", a.answerValue)
                    }
                )
            }

            val obj = JSONObject().apply {
                put("uuid", item.uuid)
                put("screeningType", item.screeningType)
                put("periodStartDate", item.periodStartDate)
                put("periodEndDate", item.periodEndDate)
                put("instrumentVersion", item.instrumentVersion)
                put("totalScore", item.totalScore)
                put("category", item.category)
                put("helpIndicator", item.helpIndicator)
                put("completedAt", item.completedAt)
                put("isDeleted", item.isDeleted)
                put("createdAt", item.createdAt)
                put("updatedAt", item.updatedAt)
                put("answers", answersArray)
            }
            itemsArray.put(obj)
        }

        val payload = JSONObject().apply {
            put("clientTimestamp", request.clientTimestamp)
            put("items", itemsArray)
        }

        executeRequest(
            endpoint = "/sync/screenings/push",
            method = "POST",
            body = payload.toString(),
            token = token
        ).mapCatching { json ->
            val data = json.getJSONObject("data")
            val syncedArray = data.getJSONArray("syncedUuids")
            val syncedList = mutableListOf<String>()
            for (i in 0 until syncedArray.length()) {
                syncedList.add(syncedArray.getString(i))
            }
            SyncScreeningBatchResponseDto(
                serverTimestamp = data.optString("serverTimestamp", Instant.now().toString()),
                syncedUuids = syncedList
            )
        }
    }

    suspend fun pullScreenings(
        token: String,
        since: Instant
    ): Result<List<ScreeningNetworkDto>> = withContext(Dispatchers.IO) {
        val query = if (since != Instant.EPOCH) "?since=${since}" else ""
        executeRequest(
            endpoint = "/sync/screenings/pull$query",
            method = "GET",
            body = null,
            token = token
        ).mapCatching { json ->
            val data = json.getJSONObject("data")
            val itemsArray = data.getJSONArray("items")
            val list = mutableListOf<ScreeningNetworkDto>()
            for (i in 0 until itemsArray.length()) {
                val item = itemsArray.getJSONObject(i)
                val answersArray = item.getJSONArray("answers")
                val answersList = mutableListOf<ScreeningAnswerNetworkDto>()
                for (j in 0 until answersArray.length()) {
                    val a = answersArray.getJSONObject(j)
                    answersList.add(
                        ScreeningAnswerNetworkDto(
                            questionNumber = a.getInt("questionNumber"),
                            answerValue = a.getInt("answerValue")
                        )
                    )
                }

                list.add(
                    ScreeningNetworkDto(
                        uuid = item.getString("uuid"),
                        screeningType = item.getString("screeningType"),
                        periodStartDate = item.getString("periodStartDate"),
                        periodEndDate = item.getString("periodEndDate"),
                        instrumentVersion = item.optString("instrumentVersion", "PHQ-9"),
                        totalScore = item.getInt("totalScore"),
                        category = item.getString("category"),
                        helpIndicator = item.getBoolean("helpIndicator"),
                        completedAt = item.getString("completedAt"),
                        isDeleted = item.optBoolean("isDeleted", false),
                        createdAt = item.getString("createdAt"),
                        updatedAt = item.getString("updatedAt"),
                        answers = answersList
                    )
                )
            }
            list
        }
    }

    private fun executeRequest(
        endpoint: String,
        method: String,
        body: String?,
        token: String?
    ): Result<JSONObject> {
        var connection: HttpURLConnection? = null
        return try {
            val fullUrl = "$baseUrl$endpoint"
            val url = URL(fullUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                if (!token.isNullOrBlank()) {
                    setRequestProperty("Authorization", "Bearer $token")
                }
                doInput = true
                if (body != null && (method == "POST" || method == "PUT" || method == "PATCH")) {
                    doOutput = true
                    OutputStreamWriter(outputStream, "UTF-8").use { writer ->
                        writer.write(body)
                        writer.flush()
                    }
                }
            }

            val responseCode = connection.responseCode
            val inputStream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: connection.inputStream
            }

            val responseText = BufferedReader(InputStreamReader(inputStream, "UTF-8")).use { it.readText() }
            val json = JSONObject(responseText)

            if (responseCode in 200..299 && json.optBoolean("success", true)) {
                Result.success(json)
            } else {
                val errorObj = json.optJSONObject("error")
                val errorMessage = errorObj?.optString("message")
                    ?: json.optString("message", "Permintaan gagal dengan status $responseCode")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseAuthResponse(data: JSONObject): AuthResponseDto {
        val token = data.getString("token")
        val userObj = data.getJSONObject("user")
        val user = AuthUserDto(
            id = userObj.getString("id"),
            email = userObj.getString("email"),
            userName = if (userObj.isNull("userName")) null else userObj.optString("userName"),
            createdAt = userObj.optString("createdAt", Instant.now().toString())
        )
        return AuthResponseDto(token = token, user = user)
    }

    companion object {
        const val DEFAULT_BASE_URL = "http://192.168.100.10:3000/api/v1"
        private const val TIMEOUT_MS = 15000
    }
}
