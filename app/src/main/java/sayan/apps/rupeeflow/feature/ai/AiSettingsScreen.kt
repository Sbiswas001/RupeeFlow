package sayan.apps.rupeeflow.feature.ai

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(
    viewModel: AiViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Settings") },
                navigationIcon = {
                    // Back button
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Model Status", style = MaterialTheme.typography.titleMedium)
            
            ListItem(
                headlineContent = { Text("Local Model Downloaded") },
                trailingContent = { 
                    Switch(checked = state.isModelDownloaded, onCheckedChange = null, enabled = false) 
                }
            )

            if (state.isModelDownloaded || state.status == AiStatus.FAILED) {
                Button(
                    onClick = { viewModel.deleteModel() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Model")
                }
            }

            HorizontalDivider()

            Text("Preferences", style = MaterialTheme.typography.titleMedium)
            
            ListItem(
                headlineContent = { Text("Download over Wi-Fi only") },
                trailingContent = {
                    Switch(
                        checked = state.wifiOnlyDownload,
                        onCheckedChange = { viewModel.toggleWifiOnly(it) }
                    )
                }
            )

            Button(onClick = { viewModel.clearError() }) {
                Text("Clear Chat History")
            }
        }
    }
}
