package sayan.apps.rupeeflow.feature.ai

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AiChatScreen(
    viewModel: AiViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        when (state.status) {
            AiStatus.CHECKING_DEVICE -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text("Checking device compatibility...")
                    }
                }
            }
            AiStatus.INCOMPATIBLE -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("AI Unavailable: ${state.error}", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(32.dp))
                }
            }
            AiStatus.NOT_DOWNLOADED, AiStatus.DOWNLOADING -> {
                DownloadModelView(
                    isDownloading = state.isDownloading,
                    progress = state.downloadProgress,
                    onDownloadClick = { viewModel.downloadModel() },
                    onDemoModeClick = { viewModel.startDemoMode() }
                )
            }
            else -> {
                if (state.error != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = state.error ?: "AI not ready.",
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (state.status == AiStatus.FAILED) {
                                TextButton(onClick = { viewModel.deleteModel() }) {
                                    Text("Retry Download")
                                }
                            }
                            TextButton(onClick = { viewModel.clearError() }) {
                                Text("Dismiss")
                            }
                        }
                    }
                }

                ChatView(
                    messages = state.messages,
                    isProcessing = state.isProcessing,
                    modifier = Modifier.weight(1f)
                )

                ChatInput(
                    text = inputText,
                    onTextChange = { inputText = it },
                    onSendClick = {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    },
                    enabled = !state.isProcessing && state.status == AiStatus.READY
                )
            }
        }
    }
}

@Composable
fun ChatView(
    messages: List<ChatMessageUi>,
    isProcessing: Boolean,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(messages) { message ->
            ChatBubble(message)
        }
        if (isProcessing) {
            item {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessageUi) {
    val alignment = if (message.isFromUser) Alignment.End else Alignment.Start
    val color = if (message.isFromUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = color
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun ChatInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Ask anything...") },
            enabled = enabled
        )
        IconButton(
            onClick = onSendClick,
            enabled = enabled && text.isNotBlank()
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
        }
    }
}

@Composable
fun DownloadModelView(
    isDownloading: Boolean,
    progress: Int,
    onDownloadClick: () -> Unit,
    onDemoModeClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "AI Assistant",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "To use the offline AI, you need to download the model (~820 MB). Your data stays private on your device.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(32.dp))
        if (isDownloading) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth()
            )
            Text("Downloading... $progress%")
        } else {
            Button(onClick = onDownloadClick) {
                Text("Download Model")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDemoModeClick) {
                Text("Try Demo Mode (Instant)")
            }
        }
    }
}
