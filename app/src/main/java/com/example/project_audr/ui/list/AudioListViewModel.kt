package com.example.project_audr.ui.list

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.project_audr.data.local.AppDatabase
import com.example.project_audr.data.local.AudioRecordEntity
import com.example.project_audr.data.repository.AudioRecordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AudioListUiState(
    val records: List<AudioRecordEntity> = emptyList(),
    val allRecords: List<AudioRecordEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val sortOrder: SortOrder = SortOrder.DATE_NEWEST,
    val filter: RecordFilter = RecordFilter.ALL,
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<Int> = emptySet()
)

class AudioListViewModel(application: Application) : AndroidViewModel(application) {

    private var repository: AudioRecordRepository? = null
    private var searchQuery: String = ""

    private val _uiState = MutableStateFlow(AudioListUiState(isLoading = true))
    val uiState: StateFlow<AudioListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val dao = withContext(Dispatchers.IO) {
                    AppDatabase.getInstance(getApplication()).audioRecordDao()
                }
                repository = AudioRecordRepository(dao)
            } catch (e: Exception) {
                _uiState.value = AudioListUiState(
                    isLoading = false,
                    error = e.message ?: "Ошибка инициализации БД"
                )
                return@launch
            }
            loadRecords()
        }
    }

    fun loadRecords() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                repository?.getAllRecords()?.collect { records ->
                    val current = _uiState.value
                    _uiState.value = current.copy(
                        allRecords = records,
                        records = applySortAndFilter(records, current.sortOrder, current.filter),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Неизвестная ошибка"
                )
            }
        }
    }

    fun searchRecords(query: String) {
        searchQuery = query
        viewModelScope.launch {
            try {
                val flow = if (query.isBlank()) {
                    repository?.getAllRecords()
                } else {
                    repository?.searchRecords(query)
                }
                flow?.collect { records ->
                    val current = _uiState.value
                    _uiState.value = current.copy(
                        allRecords = records,
                        records = applySortAndFilter(records, current.sortOrder, current.filter),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun setSortOrder(order: SortOrder) {
        val current = _uiState.value
        _uiState.value = current.copy(
            sortOrder = order,
            records = applySortAndFilter(current.allRecords, order, current.filter)
        )
    }

    fun setFilter(filter: RecordFilter) {
        val current = _uiState.value
        _uiState.value = current.copy(
            filter = filter,
            records = applySortAndFilter(current.allRecords, current.sortOrder, filter)
        )
    }

    fun renameRecord(record: AudioRecordEntity, newTitle: String) {
        viewModelScope.launch {
            try {
                val updated = record.copy(title = newTitle)
                repository?.updateRecord(updated)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun enterSelectionMode(record: AudioRecordEntity) {
        _uiState.value = _uiState.value.copy(
            isSelectionMode = true,
            selectedIds = setOf(record.id)
        )
    }

    fun toggleSelection(record: AudioRecordEntity) {
        val current = _uiState.value
        val newSelected = if (record.id in current.selectedIds) {
            current.selectedIds - record.id
        } else {
            current.selectedIds + record.id
        }

        _uiState.value = current.copy(
            selectedIds = newSelected,
            isSelectionMode = newSelected.isNotEmpty()
        )
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            isSelectionMode = false,
            selectedIds = emptySet()
        )
    }

    fun deleteSelected() {
        viewModelScope.launch {
            try {
                val current = _uiState.value
                current.selectedIds.forEach { id ->
                    val record = current.allRecords.find { it.id == id }
                    if (record != null) {
                        val file = java.io.File(record.filePath)
                        if (file.exists()) file.delete()
                        repository?.deleteRecord(record)
                    }
                }
                clearSelection()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    private fun applySortAndFilter(
        records: List<AudioRecordEntity>,
        sortOrder: SortOrder,
        filter: RecordFilter
    ): List<AudioRecordEntity> {
        val filtered = when (filter) {
            RecordFilter.ALL -> records
            RecordFilter.RECORDED -> records.filter { !it.isImported }
            RecordFilter.IMPORTED -> records.filter { it.isImported }
            RecordFilter.FAVORITES -> records.filter { it.isFavorite }
        }

        return when (sortOrder) {
            SortOrder.DATE_NEWEST -> filtered.sortedByDescending { it.dateCreated }
            SortOrder.DATE_OLDEST -> filtered.sortedBy { it.dateCreated }
            SortOrder.TITLE_ASC -> filtered.sortedBy { it.title.lowercase() }
            SortOrder.TITLE_DESC -> filtered.sortedByDescending { it.title.lowercase() }
            SortOrder.DURATION_SHORT -> filtered.sortedBy { it.duration }
            SortOrder.DURATION_LONG -> filtered.sortedByDescending { it.duration }
        }
    }

    fun toggleFavorite(record: AudioRecordEntity) {
        viewModelScope.launch {
            try {
                repository?.toggleFavorite(record.id, !record.isFavorite)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun deleteRecord(record: AudioRecordEntity) {
        viewModelScope.launch {
            try {
                val file = java.io.File(record.filePath)
                if (file.exists()) {
                    file.delete()
                }
                repository?.deleteRecord(record)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}