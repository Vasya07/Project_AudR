package com.example.project_audr.util.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    recordId: Int,
    onBackClick: () -> Unit,
    viewModel: PlayerViewModel = viewModel()
) {
    val record by viewModel.record.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val error by viewModel.error.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(recordId) {
        viewModel.loadRecord(recordId)
    }
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopAndRelease()
        }
    }

    // Диалог ошибки
    if (error != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Ошибка") },
            text = { Text(error ?: "") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }

    // Диалог удаления
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить запись?") },
            text = { Text("«${record?.title ?: ""}» будет удалена навсегда.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteRecord { onBackClick() }
                    showDeleteDialog = false
                }) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Диалог переименования
    if (showRenameDialog) {
        var newTitle by remember { mutableStateOf(record?.title ?: "") }

        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
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
                        viewModel.renameRecord(newTitle.trim())
                        showRenameDialog = false
                    }
                }) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Плеер") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    // Переименовать
                    IconButton(onClick = { showRenameDialog = true }) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Переименовать",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    // Избранное
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            imageVector = if (record?.isFavorite == true)
                                Icons.Default.Favorite
                            else
                                Icons.Default.FavoriteBorder,
                            contentDescription = "Избранное",
                            tint = if (record?.isFavorite == true)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                    // Удалить
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Удалить",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = record?.title ?: "Загрузка...",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            record?.let {
                Text(
                    text = formatDate(it.dateCreated),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Slider(
                value = if (playerState.durationMs > 0)
                    playerState.currentPositionMs.toFloat() / playerState.durationMs
                else 0f,
                onValueChange = { newValue ->
                    val positionMs = (newValue * playerState.durationMs).toInt()
                    viewModel.seekTo(positionMs)
                },
                enabled = playerState.isPrepared
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatMs(playerState.currentPositionMs))
                Text(formatMs(playerState.durationMs))
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.skipBackward() },
                    enabled = playerState.isPrepared
                ) {
                    Icon(
                        Icons.Default.Replay10,
                        contentDescription = "Назад 5 сек",
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                FilledIconButton(
                    onClick = { viewModel.playPause() },
                    enabled = playerState.isPrepared,
                    modifier = Modifier.size(80.dp)
                ) {
                    Icon(
                        imageVector = if (playerState.isPlaying)
                            Icons.Default.Stop
                        else
                            Icons.Default.PlayArrow,
                        contentDescription = if (playerState.isPlaying) "Пауза" else "Продолжить",
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                IconButton(
                    onClick = { viewModel.skipForward() },
                    enabled = playerState.isPrepared
                ) {
                    Icon(
                        Icons.Default.Forward10,
                        contentDescription = "Вперёд 5 сек",
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!playerState.isPrepared && error == null) {
                CircularProgressIndicator()
            }
        }
    }
}

private fun formatMs(ms: Int): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format(Locale.getDefault(), "%02d:%02d", min, sec)
}

private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return format.format(date)
}