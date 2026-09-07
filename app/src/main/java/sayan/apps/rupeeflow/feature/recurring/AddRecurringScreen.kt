package sayan.apps.rupeeflow.feature.recurring

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import sayan.apps.rupeeflow.core.util.CurrencyFormatter
import sayan.apps.rupeeflow.core.util.LocalUserPreferences
import sayan.apps.rupeeflow.domain.model.Account
import sayan.apps.rupeeflow.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecurringScreen(
    itemId: Long? = null,
    onNavigateBack: () -> Unit,
    viewModel: RecurringViewModel = hiltViewModel()
) {
    val preferences = LocalUserPreferences.current
    val rawCategories by viewModel.categories.collectAsState()
    val categories = remember(rawCategories) {
        rawCategories.filter { it.type == TransactionType.EXPENSE && !it.isDeleted }
    }
    val accounts by viewModel.accounts.collectAsState()
    val items by viewModel.recurringItems.collectAsState()
    
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<sayan.apps.rupeeflow.domain.model.Category?>(null) }
    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var initialDate by remember(itemId) { mutableLongStateOf(System.currentTimeMillis()) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate)
    val selectedDate = datePickerState.selectedDateMillis ?: initialDate
    val dateFormat = remember { SimpleDateFormat("d MMMM yyyy", Locale.getDefault()) }

    var frequencyInterval by remember { mutableStateOf("1") }
    var frequencyUnit by remember { mutableStateOf("Months") }
    var unitExpanded by remember { mutableStateOf(false) }
    val units = listOf("Days", "Weeks", "Months", "Years")

    var isAutoPay by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Use a flag to ensure we only populate from the item once
    var hasInitialized by remember(itemId) { mutableStateOf(false) }

    LaunchedEffect(itemId, items, categories, accounts) {
        if (itemId != null && !hasInitialized && items.isNotEmpty() && categories.isNotEmpty() && accounts.isNotEmpty()) {
            val item = items.find { it.id == itemId }
            if (item != null) {
                name = item.title
                amount = item.amount.toString()
                selectedCategory = categories.find { it.id == item.categoryId || it.name == item.category }
                selectedAccount = accounts.find { it.id == item.accountId }
                initialDate = item.dueDate
                datePickerState.selectedDateMillis = item.dueDate
                frequencyInterval = item.frequencyInterval.toString()
                frequencyUnit = item.frequencyUnit.lowercase().replaceFirstChar { it.uppercase() }
                isAutoPay = item.isAutoPay
                hasInitialized = true
            }
        }
    }

    LaunchedEffect(categories) {
        if (itemId == null && selectedCategory == null && categories.isNotEmpty()) {
            selectedCategory = categories.firstOrNull { it.name == "Bills" } ?: categories.first()
        }
    }

    LaunchedEffect(accounts) {
        if (itemId == null && selectedAccount == null && accounts.isNotEmpty()) {
            selectedAccount = accounts.firstOrNull { it.id == 1L } ?: accounts.first()
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (itemId == null) "New Recurring Payment" else "Edit Recurring Payment", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Name Field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF7C3AED),
                    unfocusedBorderColor = Color(0xFF1F2937),
                    focusedLabelColor = Color(0xFF7C3AED),
                    unfocusedLabelColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            // Amount Field
            OutlinedTextField(
                value = amount,
                onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) amount = it },
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                prefix = { Text(CurrencyFormatter.getSymbol(preferences) + " ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF7C3AED),
                    unfocusedBorderColor = Color(0xFF1F2937),
                    focusedLabelColor = Color(0xFF7C3AED),
                    unfocusedLabelColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedPrefixColor = Color.White,
                    unfocusedPrefixColor = Color.White
                )
            )

            // Category Selection
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Category", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { category ->
                        val isSelected = selectedCategory?.id == category.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            label = { Text(category.name) },
                            leadingIcon = { Text(category.icon) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF7C3AED),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1F2937),
                                labelColor = Color.Gray
                            ),
                            border = null
                        )
                    }
                }
            }

            // Account Selection
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Payment Account", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(accounts) { account ->
                        val isSelected = selectedAccount?.id == account.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedAccount = account },
                            label = { Text(account.name) },
                            leadingIcon = { Icon(Icons.Rounded.AccountBalanceWallet, null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF7C3AED),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1F2937),
                                labelColor = Color.Gray
                            ),
                            border = null
                        )
                    }
                }
            }

            // Next Payment Date
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Next payment date", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                Card(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1F2937)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = Color(0xFF7C3AED))
                            Text(dateFormat.format(Date(selectedDate)), color = Color.White)
                        }
                        Icon(Icons.Rounded.Edit, contentDescription = "Change Date", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Frequency Selection
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Frequency", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Every", color = Color.Gray)
                    
                    OutlinedTextField(
                        value = frequencyInterval,
                        onValueChange = { 
                            if (it.isEmpty() || (it.toIntOrNull() != null && it.toInt() <= 365)) {
                                frequencyInterval = it 
                            }
                        },
                        modifier = Modifier.width(80.dp),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF7C3AED),
                            unfocusedBorderColor = Color(0xFF1F2937),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )

                    ExposedDropdownMenuBox(
                        expanded = unitExpanded,
                        onExpandedChange = { unitExpanded = !unitExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = frequencyUnit,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF7C3AED),
                                unfocusedBorderColor = Color(0xFF1F2937),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = unitExpanded,
                            onDismissRequest = { unitExpanded = false },
                            modifier = Modifier.background(Color(0xFF1F2937))
                        ) {
                            units.forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit) },
                                    onClick = {
                                        frequencyUnit = unit
                                        unitExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // AutoPay
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("AutoPay", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        Text("Automatically charged when due", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Switch(
                        checked = isAutoPay, 
                        onCheckedChange = { isAutoPay = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF7C3AED)
                        )
                    )
                }
            }

            HorizontalDivider(color = Color(0xFF1F2937), thickness = 1.dp)

            // Cost Preview
            val amountVal = amount.toDoubleOrNull() ?: 0.0
            val intervalVal = frequencyInterval.toIntOrNull() ?: 1
            val (monthly, annual) = viewModel.calculateCostPreview(amountVal, intervalVal, frequencyUnit)
            
            if (amountVal > 0) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${CurrencyFormatter.format(monthly, preferences)} / month",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    val estimatePrefix = if (frequencyUnit == "Days" && (365 % intervalVal != 0)) "≈ " else ""
                    Text(
                        text = "${estimatePrefix}${CurrencyFormatter.format(annual, preferences)} / year",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }

            // Save Button
            val isValid = name.isNotBlank() && 
                          amountVal > 0 && 
                          selectedCategory != null && 
                          frequencyInterval.isNotBlank() && 
                          intervalVal > 0 &&
                          !isSaving

            Button(
                onClick = {
                    if (isValid) {
                        isSaving = true
                        if (itemId == null) {
                            viewModel.addRecurringItem(
                                title = name,
                                amount = amountVal,
                                category = selectedCategory?.name ?: "Bills",
                                categoryId = selectedCategory?.id,
                                frequency = "CUSTOM",
                                frequencyInterval = intervalVal,
                                frequencyUnit = frequencyUnit.uppercase(),
                                isAutoPay = isAutoPay,
                                dueDate = selectedDate,
                                accountId = selectedAccount?.id,
                                onComplete = onNavigateBack
                            )
                        } else {
                            viewModel.updateRecurringItem(
                                id = itemId,
                                title = name,
                                amount = amountVal,
                                category = selectedCategory?.name ?: "Bills",
                                categoryId = selectedCategory?.id,
                                frequency = "CUSTOM",
                                frequencyInterval = intervalVal,
                                frequencyUnit = frequencyUnit.uppercase(),
                                isAutoPay = isAutoPay,
                                dueDate = selectedDate,
                                accountId = selectedAccount?.id,
                                onComplete = onNavigateBack
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7C3AED),
                    disabledContainerColor = Color(0xFF1F2937),
                    disabledContentColor = Color.Gray
                )
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(if (itemId == null) "Save Recurring Payment" else "Update Recurring Payment", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

private fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
