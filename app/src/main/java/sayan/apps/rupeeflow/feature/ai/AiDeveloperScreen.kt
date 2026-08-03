package sayan.apps.rupeeflow.feature.ai

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiDeveloperScreen(
    viewModel: AiViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Diagnostics") },
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
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DiagnosticItem("Status", state.status.name)
            DiagnosticItem("Android Version OK", state.compatibilityResult?.androidVersionOk?.toString() ?: "N/A")
            DiagnosticItem("Total RAM", "${String.format("%.2f", state.compatibilityResult?.totalRamGb ?: 0.0)} GB")
            DiagnosticItem("Free Storage", "${String.format("%.2f", state.compatibilityResult?.freeStorageGb ?: 0.0)} GB")
            DiagnosticItem("GPU Support", state.compatibilityResult?.gpuOk?.toString() ?: "N/A")
            
            HorizontalDivider()

            Text("Performance Metrics", style = MaterialTheme.typography.titleMedium)
            DiagnosticItem("Model Load Time", "${state.metrics.modelLoadTimeMs} ms")
            DiagnosticItem("First Token Latency", "${state.metrics.firstTokenLatencyMs} ms")
            DiagnosticItem("Total Latency", "${state.metrics.totalLatencyMs} ms")
            DiagnosticItem("Tokens per Second", String.format("%.1f", state.metrics.tokensPerSecond))
            DiagnosticItem("Cache Hits", state.metrics.cacheHitCount.toString())
            DiagnosticItem("Cache Misses", state.metrics.cacheMissCount.toString())

            HorizontalDivider()
            
            Button(
                onClick = { /* Copy diagnostics logic */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Copy Diagnostics")
            }
        }
    }
}

@Composable
fun DiagnosticItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
}
