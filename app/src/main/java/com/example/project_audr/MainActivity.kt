package com.example.project_audr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.project_audr.ui.import.ImportScreen
import com.example.project_audr.ui.list.AudioListScreen
import com.example.project_audr.ui.theme.Project_AudRTheme
import com.example.project_audr.util.RequestPermissions
import com.example.project_audr.util.player.PlayerScreen
import com.example.project_audr.util.record.RecordScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Project_AudRTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AudRApp()
                }
            }
        }
    }
}

@Composable
fun AudRApp() {
    RequestPermissions()

    var currentScreen by remember { mutableStateOf("list") }
    var selectedRecordId by remember { mutableStateOf<Int?>(null) }

    BackHandler(enabled = currentScreen != "list") {
        currentScreen = "list"
    }

    when (currentScreen) {
        "list" -> AudioListScreen(
            onRecordClick = { record ->
                selectedRecordId = record.id
                currentScreen = "player"
            },
            onAddClick = { currentScreen = "record" },
            onImportClick = { currentScreen = "import" }
        )

        "record" -> RecordScreen(
            onBackClick = { currentScreen = "list" },
            onSaved = { currentScreen = "list" }
        )

        "import" -> ImportScreen(
            onBackClick = { currentScreen = "list" },
            onImported = { currentScreen = "list" }
        )

        "player" -> {
            val id = selectedRecordId
            if (id != null) {
                PlayerScreen(
                    recordId = id,
                    onBackClick = { currentScreen = "list" }
                )
            } else {
                LaunchedEffect(Unit) { currentScreen = "list" }
            }
        }
    }
}