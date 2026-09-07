package sayan.apps.rupeeflow.feature.transactions

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import sayan.apps.rupeeflow.core.designsystem.theme.EmeraldGreen
import sayan.apps.rupeeflow.core.designsystem.theme.VibrantRed
import sayan.apps.rupeeflow.core.util.CurrencyFormatter
import sayan.apps.rupeeflow.core.util.LocalUserPreferences
import sayan.apps.rupeeflow.domain.model.PaymentMethodType
import sayan.apps.rupeeflow.domain.model.TransactionType
import sayan.apps.rupeeflow.feature.accounts.components.AddEditCustomUpiAppDialog
import sayan.apps.rupeeflow.feature.accounts.components.AddEditDebitCardDialog
import sayan.apps.rupeeflow.feature.categories.AddEditCategoryBottomSheet
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onNavigateBack: () -> Unit,
    transactionId: Long? = null,
    modifier: Modifier = Modifier,
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    val preferences = LocalUserPreferences.current
    val amount by viewModel.amount.collectAsState()
    val title by viewModel.title.collectAsState()
    val note by viewModel.note.collectAsState()
    val isIncome by viewModel.isIncome.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val selectedAccountId by viewModel.selectedAccountId.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val selectedTimestamp by viewModel.selectedTimestamp.collectAsState()
    val attachments by viewModel.attachments.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()

    val showPaymentDetails by viewModel.showPaymentDetails.collectAsState()
    val paymentMethodType by viewModel.paymentMethodType.collectAsState()
    val upiTransactionId by viewModel.upiTransactionId.collectAsState()
    val selectedUpiAppName by viewModel.selectedUpiAppName.collectAsState()
    val selectedDebitCard by viewModel.selectedDebitCard.collectAsState()

    val upiApps by viewModel.upiApps.collectAsState()
    val debitCards by viewModel.debitCards.collectAsState()

    val attachmentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.onAddAttachment(it.toString()) }
    }

    LaunchedEffect(transactionId) {
        if (transactionId != null) {
            viewModel.loadTransaction(transactionId)
        } else {
            viewModel.resetFields()
        }
    }

    val scrollState = rememberScrollState()
    var categoryExpanded by remember { mutableStateOf(false) }
    var accountExpanded by remember { mutableStateOf(false) }
    var upiExpanded by remember { mutableStateOf(false) }
    var cardExpanded by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showAddCategorySheet by remember { mutableStateOf(false) }

    var showInlineAddUpiDialog by remember { mutableStateOf(false) }
    var showInlineAddCardDialog by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Transaction" else "Add Transaction") },
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
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Amount Input
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Amount",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextField(
                    value = amount,
                    onValueChange = viewModel::onAmountChange,
                    prefix = {
                        Text(
                            text = CurrencyFormatter.getSymbol(preferences),
                            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    textStyle = MaterialTheme.typography.displayMedium.copy(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    ),
                    placeholder = {
                        Text(
                            "0.00",
                            style = MaterialTheme.typography.displayMedium.copy(
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Income/Expense Toggle
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                SegmentedButton(
                    selected = isIncome,
                    onClick = { viewModel.onToggleIncome(true) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = EmeraldGreen.copy(alpha = 0.2f),
                        activeContentColor = EmeraldGreen
                    )
                ) {
                    Text("Income")
                }
                SegmentedButton(
                    selected = !isIncome,
                    onClick = { viewModel.onToggleIncome(false) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = VibrantRed.copy(alpha = 0.2f),
                        activeContentColor = VibrantRed
                    )
                ) {
                    Text("Expense")
                }
            }

            // Title Field
            OutlinedTextField(
                value = title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("Title") },
                placeholder = { Text("e.g. Salary, Lunch") },
                leadingIcon = { Icon(Icons.Rounded.Title, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            // Account Dropdown
            ExposedDropdownMenuBox(
                expanded = accountExpanded,
                onExpandedChange = { accountExpanded = !accountExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                val selectedAccount = accounts.find { it.id == selectedAccountId }
                OutlinedTextField(
                    value = selectedAccount?.name ?: "Select Account",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Account") },
                    leadingIcon = { Icon(Icons.Rounded.AccountBalance, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                    modifier = Modifier
                        .menuAnchor(type = MenuAnchorType.PrimaryEditable, enabled = true)
                        .fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                ExposedDropdownMenu(
                    expanded = accountExpanded,
                    onDismissRequest = { accountExpanded = false }
                ) {
                    accounts.forEach { account ->
                        DropdownMenuItem(
                            text = { Text(account.name) },
                            onClick = {
                                viewModel.onAccountChange(account.id)
                                accountExpanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }

            // Category Dropdown
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                val selectedCategory = categories.find { it.id == selectedCategoryId }
                OutlinedTextField(
                    value = selectedCategory?.name ?: "Select Category",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    leadingIcon = { 
                        if (selectedCategory != null) {
                            Text(selectedCategory.icon, modifier = Modifier.padding(start = 12.dp))
                        } else {
                            Icon(Icons.Rounded.Category, contentDescription = null)
                        }
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier
                        .menuAnchor(type = MenuAnchorType.PrimaryEditable, enabled = true)
                        .fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(category.icon)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(category.name)
                                }
                            },
                            onClick = {
                                viewModel.onCategoryChange(category.id)
                                categoryExpanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Add new Category",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        },
                        onClick = {
                            categoryExpanded = false
                            showAddCategorySheet = true
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }

            // Date & Time Selectors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedCard(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = dateFormat.format(Date(selectedTimestamp)), style = MaterialTheme.typography.bodyMedium)
                    }
                }

                OutlinedCard(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = timeFormat.format(Date(selectedTimestamp)), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Payment Details Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = showPaymentDetails,
                    onCheckedChange = { viewModel.onTogglePaymentDetails(it) }
                )
                Text("Add UPI/Card Details", style = MaterialTheme.typography.bodyMedium)
            }

            if (showPaymentDetails) {
                // Payment Method Selector
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SegmentedButton(
                        selected = paymentMethodType == PaymentMethodType.UPI,
                        onClick = { viewModel.onPaymentMethodTypeChange(PaymentMethodType.UPI) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("📱 UPI")
                    }
                    SegmentedButton(
                        selected = paymentMethodType == PaymentMethodType.DEBIT_CARD,
                        onClick = { viewModel.onPaymentMethodTypeChange(PaymentMethodType.DEBIT_CARD) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("💳 Debit Card")
                    }
                }

                if (paymentMethodType == PaymentMethodType.UPI) {
                    // UPI App Dropdown
                    ExposedDropdownMenuBox(
                        expanded = upiExpanded,
                        onExpandedChange = { upiExpanded = !upiExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedUpiAppName ?: "Select UPI App",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("UPI App") },
                            leadingIcon = { Icon(Icons.Rounded.Smartphone, contentDescription = null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = upiExpanded) },
                            modifier = Modifier
                                .menuAnchor(type = MenuAnchorType.PrimaryEditable, enabled = true)
                                .fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        )
                        ExposedDropdownMenu(
                            expanded = upiExpanded,
                            onDismissRequest = { upiExpanded = false }
                        ) {
                            upiApps.forEach { app ->
                                DropdownMenuItem(
                                    text = { Text(app.appName) },
                                    onClick = {
                                        viewModel.onUpiAppNameChange(app.appName)
                                        upiExpanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("+ Add UPI App", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                                    }
                                },
                                onClick = {
                                    upiExpanded = false
                                    showInlineAddUpiDialog = true
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }

                    // UPI Transaction ID Input
                    OutlinedTextField(
                        value = upiTransactionId,
                        onValueChange = viewModel::onUPITransactionIdChange,
                        label = { Text("UPI Transaction ID") },
                        leadingIcon = { Icon(Icons.Rounded.QrCode, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )
                } else if (paymentMethodType == PaymentMethodType.DEBIT_CARD) {
                    // Debit Card Dropdown
                    ExposedDropdownMenuBox(
                        expanded = cardExpanded,
                        onExpandedChange = { cardExpanded = !cardExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val cardText = selectedDebitCard?.let { "${it.cardName} •••• ${it.last4Digits}" } 
                            ?: if (debitCards.isEmpty()) "No debit cards saved" else "Select Debit Card"

                        OutlinedTextField(
                            value = cardText,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Debit Card") },
                            leadingIcon = { Icon(Icons.Rounded.CreditCard, contentDescription = null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cardExpanded) },
                            modifier = Modifier
                                .menuAnchor(type = MenuAnchorType.PrimaryEditable, enabled = true)
                                .fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        )
                        ExposedDropdownMenu(
                            expanded = cardExpanded,
                            onDismissRequest = { cardExpanded = false }
                        ) {
                            debitCards.forEach { card ->
                                DropdownMenuItem(
                                    text = { Text("${card.cardName} •••• ${card.last4Digits}") },
                                    onClick = {
                                        viewModel.onDebitCardChange(card)
                                        cardExpanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("+ Add Debit Card", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                                    }
                                },
                                onClick = {
                                    cardExpanded = false
                                    showInlineAddCardDialog = true
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }
            }

            // Note Field
            OutlinedTextField(
                value = note,
                onValueChange = viewModel::onNoteChange,
                label = { Text("Note") },
                placeholder = { Text("Add a note...") },
                leadingIcon = { Icon(Icons.Rounded.Note, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            // Attachments Section
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Attachments", style = MaterialTheme.typography.titleSmall)
                    TextButton(onClick = { attachmentLauncher.launch("image/*") }) {
                        Icon(Icons.Rounded.AddAPhoto, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add")
                    }
                }

                if (attachments.isNotEmpty()) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        attachments.forEach { uri ->
                            Box(modifier = Modifier.size(100.dp)) {
                                Card(
                                    modifier = Modifier.fillMaxSize(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Rounded.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.onRemoveAttachment(uri) },
                                    modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                                ) {
                                    Icon(Icons.Rounded.Cancel, contentDescription = null, tint = VibrantRed)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Save Button
            Button(
                onClick = {
                    viewModel.saveTransaction {
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.large,
                enabled = amount.isNotEmpty() && selectedAccountId != null && selectedCategoryId != null
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEditMode) "Update Transaction" else "Save Transaction",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedTimestamp)
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { 
                            // Preserve time part
                            val calendar = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
                            val timePart = calendar.get(Calendar.HOUR_OF_DAY) * 3600000L + 
                                           calendar.get(Calendar.MINUTE) * 60000L + 
                                           calendar.get(Calendar.SECOND) * 1000L + 
                                           calendar.get(Calendar.MILLISECOND)
                            viewModel.onTimestampChange(it + timePart) 
                        }
                        showDatePicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        if (showTimePicker) {
            val calendar = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
            val timePickerState = rememberTimePickerState(
                initialHour = calendar.get(Calendar.HOUR_OF_DAY),
                initialMinute = calendar.get(Calendar.MINUTE)
            )
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = selectedTimestamp
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                        }
                        viewModel.onTimestampChange(cal.timeInMillis)
                        showTimePicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                },
                title = { Text("Select Time") },
                text = { TimePicker(state = timePickerState) }
            )
        }

        if (showAddCategorySheet) {
            val currentType = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE
            AddEditCategoryBottomSheet(
                defaultType = currentType,
                existingCategories = categories,
                onDismiss = { showAddCategorySheet = false },
                onConfirm = { newCategory ->
                    viewModel.addCategory(newCategory)
                    showAddCategorySheet = false
                }
            )
        }

        if (showInlineAddUpiDialog) {
            selectedAccountId?.let { accountId ->
                AddEditCustomUpiAppDialog(
                    accountId = accountId,
                    onDismiss = { showInlineAddUpiDialog = false },
                    onConfirm = { app ->
                        viewModel.addCustomUpiAppInline(app.appName, app.appPackage)
                        showInlineAddUpiDialog = false
                    }
                )
            }
        }

        if (showInlineAddCardDialog) {
            selectedAccountId?.let { accountId ->
                AddEditDebitCardDialog(
                    accountId = accountId,
                    onDismiss = { showInlineAddCardDialog = false },
                    onConfirm = { card ->
                        viewModel.addDebitCardInline(card.cardName, card.last4Digits, card.network, card.nickname)
                        showInlineAddCardDialog = false
                    }
                )
            }
        }
    }
}
