package com.example.selaras.features.breathing

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.selaras.domain.model.BreathingPattern
import com.example.selaras.domain.repository.BreathingRepository
import com.example.selaras.domain.usecase.breathing.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class BreathingPage { CATALOG, FORM, SESSION }

data class BreathingUiState(
    val page: BreathingPage = BreathingPage.CATALOG,
    val customPatterns: List<BreathingPattern> = emptyList(),
    val selectedKey: String? = null,
    val sessionPattern: BreathingPattern? = null,
    val session: BreathingSessionSnapshot = BreathingSessionSnapshot(),
    val editingId: Long? = null,
    val formName: String = "",
    val formInhale: String = "4",
    val formExhale: String = "6",
    val isSaving: Boolean = false,
    val error: String? = null
) {
    val allPatterns: List<BreathingPattern>
        get() = BreathingPattern.defaults + customPatterns
    val selectedPattern: BreathingPattern?
        get() = allPatterns.firstOrNull { it.key == selectedKey }
}

/** UI events flow through this ViewModel; no breathing session history is persisted. */
class BreathingViewModel(repository: BreathingRepository) : ViewModel() {
    private val getPatterns = GetBreathingPatternUseCase(repository)
    private val savePattern = SaveCustomBreathingPatternUseCase(repository)
    private val updatePattern = UpdateCustomBreathingPatternUseCase(repository)
    private val deletePattern = DeleteCustomBreathingPatternUseCase(repository)

    private val _uiState = MutableStateFlow(BreathingUiState())
    val uiState = _uiState.asStateFlow()
    private var engine: BreathingSessionEngine? = null
    private var ticker: Job? = null

    init {
        viewModelScope.launch {
            try {
                getPatterns().collect { patterns ->
                    // Keep a newly saved selection while Room's Flow delivers its insert.
                    _uiState.update { state -> state.copy(customPatterns = patterns) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update { it.copy(error = "Pola kustom belum dapat dimuat: ${error.message}") }
            }
        }
    }

    fun selectPattern(key: String) {
        _uiState.update { state ->
            if (state.allPatterns.any { it.key == key }) state.copy(selectedKey = key, error = null) else state
        }
    }

    fun openCreatePattern() = _uiState.update {
        it.copy(page = BreathingPage.FORM, editingId = null, formName = "",
            formInhale = "4", formExhale = "6", error = null)
    }

    fun openEditPattern(id: Long) {
        val pattern = _uiState.value.customPatterns.firstOrNull { it.id == id } ?: return
        _uiState.update {
            it.copy(page = BreathingPage.FORM, editingId = id, formName = pattern.name,
                formInhale = pattern.inhaleSeconds.toString(),
                formExhale = pattern.exhaleSeconds.toString(), error = null)
        }
    }

    fun setName(value: String) = _uiState.update { it.copy(formName = value.take(60), error = null) }
    fun setInhale(value: String) = _uiState.update {
        it.copy(formInhale = value.filter(Char::isDigit).take(3), error = null)
    }
    fun setExhale(value: String) = _uiState.update {
        it.copy(formExhale = value.filter(Char::isDigit).take(3), error = null)
    }
    fun adjustInhale(change: Int) = setInhale(adjust(_uiState.value.formInhale, change))
    fun adjustExhale(change: Int) = setExhale(adjust(_uiState.value.formExhale, change))
    private fun adjust(value: String, change: Int): String =
        ((value.toIntOrNull() ?: 1) + change).coerceIn(1, 60).toString()

    fun saveCustomPattern() {
        val state = _uiState.value
        if (state.isSaving) return
        val inhale = state.formInhale.toIntOrNull()
        val exhale = state.formExhale.toIntOrNull()
        val name = state.formName.trim()
        val error = when {
            name.length !in 1..40 -> "Nama pola harus 1–40 karakter."
            inhale == null || inhale !in 1..60 -> "Tarik napas harus 1–60 detik."
            exhale == null || exhale !in 1..60 -> "Embus napas harus 1–60 detik."
            else -> null
        }
        if (error != null) {
            _uiState.update { it.copy(error = error) }
            return
        }
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                if (state.editingId == null) {
                    val newPattern = BreathingPattern(
                        key = "temp",
                        name = name,
                        inhaleSeconds = requireNotNull(inhale),
                        exhaleSeconds = requireNotNull(exhale)
                    )
                    savePattern(newPattern).fold(
                        onSuccess = { saved ->
                            _uiState.update {
                                it.copy(
                                    isSaving = false,
                                    page = BreathingPage.CATALOG,
                                    selectedKey = saved.key,
                                    editingId = null,
                                    error = null
                                )
                            }
                        },
                        onFailure = { err ->
                            _uiState.update { it.copy(isSaving = false, error = "Gagal menyimpan pola: ${err.message}") }
                        }
                    )
                } else {
                    val existing = state.customPatterns.firstOrNull { it.id == state.editingId }
                    val updatedPattern = existing?.copy(
                        name = name,
                        inhaleSeconds = requireNotNull(inhale),
                        exhaleSeconds = requireNotNull(exhale)
                    ) ?: BreathingPattern(
                        key = "custom:${state.editingId}",
                        id = state.editingId,
                        name = name,
                        inhaleSeconds = requireNotNull(inhale),
                        exhaleSeconds = requireNotNull(exhale)
                    )
                    updatePattern(updatedPattern).fold(
                        onSuccess = {
                            _uiState.update {
                                it.copy(
                                    isSaving = false,
                                    page = BreathingPage.CATALOG,
                                    selectedKey = "custom:${state.editingId}",
                                    editingId = null,
                                    error = null
                                )
                            }
                        },
                        onFailure = { err ->
                            _uiState.update { it.copy(isSaving = false, error = "Gagal memperbarui pola: ${err.message}") }
                        }
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update { it.copy(isSaving = false, error = "Gagal menyimpan pola: ${error.message}") }
            }
        }
    }

    fun deleteCustomPattern() {
        val id = _uiState.value.editingId ?: return
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                deletePattern(id).fold(
                    onSuccess = {
                        _uiState.update {
                            it.copy(
                                page = BreathingPage.CATALOG,
                                editingId = null,
                                isSaving = false,
                                selectedKey = it.selectedKey?.takeUnless { key -> key == "custom:$id" }
                            )
                        }
                    },
                    onFailure = { err ->
                        _uiState.update { it.copy(isSaving = false, error = "Gagal menghapus pola: ${err.message}") }
                    }
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update { it.copy(isSaving = false, error = "Gagal menghapus pola: ${error.message}") }
            }
        }
    }

    fun openSession() {
        val pattern = _uiState.value.selectedPattern ?: return
        stopTicker()
        engine = BreathingSessionEngine(pattern)
        _uiState.update {
            it.copy(page = BreathingPage.SESSION, sessionPattern = pattern,
                session = requireNotNull(engine).snapshot(), error = null)
        }
    }

    fun startOrResumeSession() {
        val current = engine ?: return
        when (current.snapshot().runState) {
            BreathingRunState.READY -> current.start()
            BreathingRunState.PAUSED -> current.resume()
            BreathingRunState.RUNNING -> return
        }
        _uiState.update { it.copy(session = current.snapshot()) }
        runTicker(current)
    }

    fun pauseSession() {
        val current = engine ?: return
        current.pause()
        stopTicker()
        _uiState.update { it.copy(session = current.snapshot()) }
    }

    /** Automatically pause when the user switches away from the app. */
    fun onAppBackgrounded() {
        if (_uiState.value.page == BreathingPage.SESSION) pauseSession()
    }

    fun stopSession() {
        stopTicker()
        engine?.stop()
        engine = null
        _uiState.update {
            it.copy(page = BreathingPage.CATALOG, sessionPattern = null,
                session = BreathingSessionSnapshot(), error = null)
        }
    }

    fun goBack() = when (_uiState.value.page) {
        BreathingPage.SESSION -> stopSession()
        BreathingPage.FORM -> _uiState.update { it.copy(page = BreathingPage.CATALOG, error = null) }
        BreathingPage.CATALOG -> Unit
    }

    private fun runTicker(current: BreathingSessionEngine) {
        stopTicker()
        ticker = viewModelScope.launch {
            var previous = SystemClock.elapsedRealtime()
            while (isActive) {
                delay(32L)
                val now = SystemClock.elapsedRealtime()
                current.advanceBy(now - previous)
                previous = now
                _uiState.update { it.copy(session = current.snapshot()) }
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    override fun onCleared() {
        stopTicker()
        super.onCleared()
    }

    class Factory(private val repository: BreathingRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(BreathingViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return BreathingViewModel(repository) as T
        }
    }
}
