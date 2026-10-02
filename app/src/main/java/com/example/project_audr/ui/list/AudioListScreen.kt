package com.example.project_audr.ui.list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.project_audr.data.local.AudioRecordEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioListScreen(
    onRecordClick: (AudioRecordEntity) -> Unit = {},
    onAddClick: () -> Unit = {},
    onImportClick: () -> Unit = {},
    viewModel: AudioListViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var recordToDelete by remember { mutableStateOf<AudioRecordEntity?>(null) }
    var recordToRename by remember { mutableStateOf<AudioRecordEntity?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            if (uiState.isSelectionMode) {
                // Режим множественного выбора
                TopAppBar(
                    title = { Text("Выбрано: ${uiState.selectedIds.size}") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Отмена")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.deleteSelected() }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Удалить",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        titleContentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                )
            } else {
                // Обычный режим
                TopAppBar(
                    title = { Text("AudR — Голосовые заметки") },
                    actions = {
                        // Импорт
                        IconButton(onClick = onImportClick) {
                            Icon(Icons.Default.Upload, contentDescription = "Импорт")
                        }
                        // Сортировка
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Сортировка")
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                SortOrder.entries.forEach { order ->
                                    DropdownMenuItem(
                                        text = { Text(order.label) },
                                        leadingIcon = {
                                            if (uiState.sortOrder == order) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.setSortOrder(order)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                        // Фильтр
                        Box {
                            IconButton(onClick = { showFilterMenu = true }) {
                                Icon(Icons.Default.FilterList, contentDescription = "Фильтр")
                            }
                            DropdownMenu(
                                expanded = showFilterMenu,
                                onDismissRequest = { showFilterMenu = false }
                            ) {
                                RecordFilter.entries.forEach { filter ->
                                    DropdownMenuItem(
                                        text = { Text(filter.label) },
                                        leadingIcon = {
                                            if (uiState.filter == filter) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.setFilter(filter)
                                            showFilterMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Новая запись")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.searchRecords(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Поиск записей...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Ошибка: ${uiState.error}",
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadRecords() }) {
                                Text("Повторить")
                            }
                        }
                    }
                }

                uiState.records.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Нет записей",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Нажмите + чтобы создать первую запись",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.records, key = { it.id }) { record ->
                            AudioRecordCard(
                                record = record,
                                onClick = {
                                    if (uiState.isSelectionMode) {
                                        viewModel.toggleSelection(record)
                                    } else {
                                        onRecordClick(record)
                                    }
                                },
                                onLongClick = { viewModel.enterSelectionMode(record) },
                                onFavoriteClick = { viewModel.toggleFavorite(record) },
                                onDeleteClick = { recordToDelete = record },
                                onRenameClick = { recordToRename = record },
                                isSelected = record.id in uiState.selectedIds
                            )
                        }
                    }
                }
            }

            // Диалог переименования с проверкой на пустоту
            recordToRename?.let { record ->
                var newTitle by remember { mutableStateOf(record.title) }

                AlertDialog(
                    onDismissRequest = { recordToRename = null },
                    title = { Text("Переименовать") },
                    text = {
                        OutlinedTextField(
                            value = newTitle,
                            onValueChange = { newTitle = it },
                            label = { Text("Новое название") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            isError = newTitle.isBlank()
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            if (newTitle.isBlank()) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Название записи не может быть пустым",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            } else {
                                viewModel.renameRecord(record, newTitle.trim())
                                recordToRename = null
                            }
                        }) {
                            Text("Сохранить")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { recordToRename = null }) {
                            Text("Отмена")
                        }
                    }
                )
            }

            // Диалог удаления
            recordToDelete?.let { record ->
                AlertDialog(
                    onDismissRequest = { recordToDelete = null },
                    title = { Text("Удалить запись?") },
                    text = { Text("«${record.title}» будет удалена навсегда.") },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.deleteRecord(record)
                            recordToDelete = null
                        }) {
                            Text("Удалить", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { recordToDelete = null }) {
                            Text("Отмена")
                        }
                    }
                )
            }
        }
    }
}