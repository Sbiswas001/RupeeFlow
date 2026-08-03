package sayan.apps.rupeeflow.feature.recurring

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import sayan.apps.rupeeflow.core.util.CurrencyFormatter
import sayan.apps.rupeeflow.core.util.LocalUserPreferences
import sayan.apps.rupeeflow.domain.model.RecurringItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecurringScreen(
    onNavigateBack: () -> Unit,
    viewModel: RecurringViewModel = hiltViewModel()
) {
    val preferences = LocalUserPreferences.current
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Bills") }
    var frequency by remember { mutableStateOf("MONTHLY") }
    var isAutoPay by remember { mutableStateOf(false) }

    val templates = listOf(
        RecurringTemplate("Netflix", "Subscriptions", Icons.Rounded.Subscriptions),
        RecurringTemplate("Spotify", "Subscriptions", Icons.Rounded.MusicNote),
        RecurringTemplate("Jio Fiber", "Bills", Icons.Rounded.Wifi),
        RecurringTemplate("Airtel Fiber", "Bills", Icons.Rounded.Wifi),
        RecurringTemplate("Google One", "Subscriptions", Icons.Rounded.Cloud),
        RecurringTemplate("Amazon Prime", "Subscriptions", Icons.Rounded.ShoppingBag),
        RecurringTemplate("YouTube Premium", "Subscriptions", Icons.Rounded.PlayArrow),
        RecurringTemplate("ChatGPT", "Subscriptions", Icons.Rounded.SmartToy),
        RecurringTemplate("LIC Premium", "Insurance", Icons.Rounded.Security),
        RecurringTemplate("SIP", "SIP", Icons.Rounded.TrendingUp),
        RecurringTemplate("Rent", "Bills", Icons.Rounded.Home),
        RecurringTemplate("Credit Card", "Credit Cards", Icons.Rounded.CreditCard)
    )

    Scaffold(
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("Templates", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(templates) { template ->
                    TemplateItem(template) {
                        name = template.name
                        category = template.category
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                prefix = { Text(CurrencyFormatter.getSymbol(preferences)) }
            )

            // Category & Frequency Dropdowns would go here
            // For brevity, using simple text selection for now

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("AutoPay Enabled")
                Switch(checked = isAutoPay, onCheckedChange = { isAutoPay = it })
            }

            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && amountValue > 0) {
                        viewModel.addRecurringItem(
                            title = name,
                            amount = amountValue,
                            category = category,
                            frequency = frequency,
                            isAutoPay = isAutoPay
                        )
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = name.isNotBlank() && amount.toDoubleOrNull() != null
            ) {
                Text("Save Recurring Payment")
            }
        }
    }
}

data class RecurringTemplate(
    val name: String,
    val category: String,
    val icon: ImageVector
)

@Composable
fun TemplateItem(template: RecurringTemplate, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.width(100.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(template.icon, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(template.name, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}
