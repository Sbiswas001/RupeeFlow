package sayan.apps.rupeeflow.feature.accounts

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import sayan.apps.rupeeflow.core.designsystem.theme.RupeeFlowTheme
import sayan.apps.rupeeflow.core.util.CurrencyFormatter
import sayan.apps.rupeeflow.core.util.DateUtils
import sayan.apps.rupeeflow.core.util.LocalUserPreferences
import sayan.apps.rupeeflow.domain.model.Account
import sayan.apps.rupeeflow.domain.model.UserPreferences
import androidx.compose.ui.draw.rotate
import sayan.apps.rupeeflow.feature.accounts.components.AddAccountBottomSheet
import sayan.apps.rupeeflow.feature.accounts.components.AccountTypeItem
import sayan.apps.rupeeflow.feature.accounts.components.AddEditCustomUpiAppDialog
import sayan.apps.rupeeflow.feature.accounts.components.AddEditDebitCardDialog
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    onNavigateToNetWorth: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val preferences = LocalUserPreferences.current
    val accounts by viewModel.accounts.collectAsState()
    var showAddBottomSheet by remember { mutableStateOf(false) }
    var selectedTypeItem by remember { mutableStateOf<AccountTypeItem?>(null) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var accountToClose by remember { mutableStateOf<Account?>(null) }
    var accountToReconcile by remember { mutableStateOf<Account?>(null) }
    var accountToEdit by remember { mutableStateOf<Account?>(null) }
    var selectedCategory by remember { mutableStateOf("All") }
    var isFabExpanded by remember { mutableStateOf(false) }

    val categories = listOf("All", "Banks", "Investments", "Credit", "Cash", "Liabilities", "Assets")
    
    val netWorth = accounts.filter { it.category != "LIABILITIES" }.sumOf { it.balance } - 
                   accounts.filter { it.category == "LIABILITIES" }.sumOf { it.balance }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Black
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(bottom = 100.dp, top = 16.dp)
            ) {
                // Net Worth Hero Card
                item {
                    NetWorthHero(
                        netWorth = netWorth, 
                        accountCount = accounts.size,
                        preferences = preferences,
                        onClick = onNavigateToNetWorth
                    )
                }

                // Category Chips
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { category ->
                            val isSelected = selectedCategory == category
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = category },
                                label = { Text(category) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RupeeFlowTheme.colors.income,
                                    selectedLabelColor = Color.Black,
                                    containerColor = Color.Transparent,
                                    labelColor = Color(0xFF9CA3AF)
                                ),
                                border = if (isSelected) null else FilterChipDefaults.filterChipBorder(
                                    borderColor = Color(0xFF1F2937),
                                    borderWidth = 1.dp,
                                    enabled = true,
                                    selected = false
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                // Account Sections
                val filteredAccounts = if (selectedCategory == "All") accounts else {
                    accounts.filter { account ->
                        when (selectedCategory) {
                            "Banks" -> account.category == "BANKING" || account.category == "DEPOSITS"
                            "Cash" -> account.category == "CASH_WALLETS"
                            "Investments" -> account.category == "INVESTMENTS"
                            "Credit" -> account.category == "CREDIT"
                            "Liabilities" -> account.category == "LIABILITIES"
                            "Assets" -> account.category == "ASSETS"
                            else -> true
                        }
                    }
                }
                
                val groupedAccounts = filteredAccounts.groupBy { it.category }
                
                if (accounts.isEmpty()) {
                    item {
                        EmptyAccountsState()
                    }
                } else if (filteredAccounts.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            Text("No accounts in this category", color = Color.Gray)
                        }
                    }
                }

                groupedAccounts.forEach { (category, accountList) ->
                    item {
                        Text(
                            text = category.replace("_", " ").lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 24.dp),
                            color = Color(0xFF9CA3AF)
                        )
                    }
                    items(accountList, key = { it.id }) { account ->
                        AccountItem(
                            account = account,
                            preferences = preferences,
                            onClick = { accountToReconcile = account },
                            onLongClick = { accountToEdit = account }
                        )
                    }
                }
            }

            // Expanding FAB (One UI Style)
            Box(modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp)) {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    AnimatedVisibility(visible = isFabExpanded) {
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SmallFABItem("Add Account", Icons.Rounded.AccountBalance) { 
                                showAddBottomSheet = true
                                isFabExpanded = false
                            }
                            SmallFABItem("Move Money", Icons.Rounded.SwapHoriz) { 
                                showTransferDialog = true
                                isFabExpanded = false
                            }
                        }
                    }
                    FloatingActionButton(
                        onClick = { isFabExpanded = !isFabExpanded },
                        containerColor = Color(0xFF7C3AED),
                        contentColor = Color.White,
                        shape = CircleShape
                    ) {
                        val rotation by animateFloatAsState(if (isFabExpanded) 45f else 0f, label = "rot")
                        Icon(
                            Icons.Rounded.Add, 
                            contentDescription = null, 
                            modifier = Modifier.rotate(rotation)
                        )
                    }
                }
            }
        }

        if (showAddBottomSheet) {
            AddAccountBottomSheet(
                onDismiss = { showAddBottomSheet = false },
                onSelectType = { item ->
                    selectedTypeItem = item
                    showAddBottomSheet = false
                }
            )
        }

        selectedTypeItem?.let { item: AccountTypeItem ->
            DynamicAddAccountDialog(
                typeItem = item,
                onDismiss = { selectedTypeItem = null },
                onConfirm = { name: String, balance: Double, institution: String?, last4: String?, limit: Double?, rate: Double?, maturity: Long? ->
                    viewModel.addAccount(
                        name = name,
                        category = item.category,
                        subType = item.subType,
                        balance = balance,
                        institutionName = institution,
                        accountNumberLast4 = last4,
                        creditLimit = limit,
                        interestRate = rate,
                        maturityDate = maturity
                    )
                    selectedTypeItem = null
                }
            )
        }

        if (showTransferDialog && accounts.size >= 2) {
            TransferDialog(
                accounts = accounts,
                onDismiss = { showTransferDialog = false },
                onTransfer = { from, to, amount, note, timestamp ->
                    viewModel.transferFunds(from, to, amount, note.ifBlank { null }, timestamp)
                    showTransferDialog = false
                }
            )
        }

        accountToClose?.let { account ->
            DeleteConfirmationDialog(
                title = "Close Account",
                message = "Are you sure you want to close '${account.name}'? This will hide it from selection but preserve its transaction history and impact on your net worth.",
                onConfirm = {
                    viewModel.deleteAccount(account)
                    accountToClose = null
                },
                onDismiss = { accountToClose = null }
            )
        }

        accountToReconcile?.let { account ->
            ReconcileBalanceDialog(
                account = account,
                preferences = preferences,
                onDismiss = { accountToReconcile = null },
                onConfirm = { actualBalance, reason, note ->
                    viewModel.reconcileAccount(account.id, actualBalance, reason, note)
                    accountToReconcile = null
                }
            )
        }

        accountToEdit?.let { account ->
            EditAccountDialog(
                account = account,
                onDismiss = { accountToEdit = null },
                onConfirm = { updatedAccount ->
                    viewModel.updateAccount(updatedAccount)
                    accountToEdit = null
                },
                onCloseAccount = {
                    accountToClose = account
                    accountToEdit = null
                }
            )
        }
    }
}

@Composable
fun NetWorthHero(netWorth: Double, accountCount: Int, preferences: UserPreferences, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth().clickable { onClick() },
        color = Color(0xFF181818),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Net Worth", style = MaterialTheme.typography.labelLarge, color = Color(0xFF9CA3AF))
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = CurrencyFormatter.format(netWorth, preferences),
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.TrendingUp, contentDescription = null, tint = RupeeFlowTheme.colors.income, modifier = Modifier.size(16.dp))
                    Text(
                        text = CurrencyFormatter.format(5240.0, preferences), 
                        style = MaterialTheme.typography.labelMedium, 
                        color = RupeeFlowTheme.colors.income
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "$accountCount Active Accounts",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF9CA3AF)
            )
        }
    }
}

@Composable
fun SmallFABItem(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(color = Color(0xFF1E1E1E), shape = RoundedCornerShape(8.dp)) {
            Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = Color.White)
        }
        Spacer(modifier = Modifier.width(12.dp))
        FloatingActionButton(
            onClick = onClick,
            containerColor = Color(0xFF1E1E1E),
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountItem(
    account: Account,
    preferences: UserPreferences,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = Color(0xFF181818),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF1E1E1E),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        getIconForAccount(account.subType),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "${account.institutionName ?: ""} ${account.accountNumberLast4?.let { "•••• $it" } ?: ""}".trim().ifEmpty { account.subType.replace("_", " ") },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF9CA3AF)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Tap to reconcile • Long press to edit",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = Color(0xFF6B7280)
                )
            }

            val sevenDaysAgo = DateUtils.getTimestampDaysAgo(7)
            val lastRec = account.lastReconciledAt

            val statusText = if (lastRec == null) {
                "⚠ Needs reconciliation"
            } else if (lastRec < sevenDaysAgo) {
                "⚠ Reconciliation due (7+ days)"
            } else {
                val lastReconciled = Calendar.getInstance().apply { timeInMillis = lastRec }
                val now = Calendar.getInstance()
                val isToday = now.get(Calendar.YEAR) == lastReconciled.get(Calendar.YEAR) &&
                        now.get(Calendar.DAY_OF_YEAR) == lastReconciled.get(Calendar.DAY_OF_YEAR)
                if (isToday) "✓ Reconciled today" else {
                    val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
                    "Last checked: ${sdf.format(Date(lastRec))}"
                }
            }

            val statusColor = if (lastRec == null || lastRec < sevenDaysAgo) {
                RupeeFlowTheme.colors.warning
            } else {
                val lastReconciled = Calendar.getInstance().apply { timeInMillis = lastRec }
                val now = Calendar.getInstance()
                val isToday = now.get(Calendar.YEAR) == lastReconciled.get(Calendar.YEAR) &&
                        now.get(Calendar.DAY_OF_YEAR) == lastReconciled.get(Calendar.DAY_OF_YEAR)
                if (isToday) RupeeFlowTheme.colors.income else Color(0xFF9CA3AF)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyFormatter.format(account.balance, preferences),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (account.category == "LIABILITIES") RupeeFlowTheme.colors.expense else Color.White
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

private fun getReconciliationStatus(account: Account): Pair<String, Color> {
    // This is not @Composable, so we'll need to pass the colors or use the constants
    // However, the caller IS @Composable (AccountItem).
    // Let's refactor this to be a @Composable or return the color in the UI.
    return "Dummy" to Color.White // Will replace actual logic below
}

fun getIconForAccount(subType: String) = when (subType) {
    "SAVINGS", "CURRENT" -> Icons.Rounded.AccountBalance
    "CASH" -> Icons.Rounded.Payments
    "WALLET" -> Icons.Rounded.AccountBalanceWallet
    "CREDIT_CARD" -> Icons.Rounded.CreditCard
    "FD", "RD" -> Icons.Rounded.AccountBalance
    "MUTUAL_FUND" -> Icons.AutoMirrored.Rounded.ShowChart
    "STOCKS" -> Icons.AutoMirrored.Rounded.ShowChart
    "GOLD" -> Icons.Rounded.MilitaryTech
    "LOAN" -> Icons.Rounded.Home
    "ASSET" -> Icons.Rounded.Apartment
    else -> Icons.Rounded.Category
}

@Composable
fun EmptyAccountsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Rounded.AccountBalance,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color(0xFF1F2937)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No accounts yet",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
        Text(
            "Add your bank accounts, wallets, or investments to see your net worth.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 48.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun DynamicAddAccountDialog(
    typeItem: AccountTypeItem,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String?, String?, Double?, Double?, Long?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var institution by remember { mutableStateOf("") }
    var last4 by remember { mutableStateOf("") }
    var limit by remember { mutableStateOf("") }
    var interestRate by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF181818),
        title = { Text("New ${typeItem.label}", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (e.g. SBI Savings)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF7C3AED),
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color(0xFF7C3AED),
                        unfocusedLabelColor = Color.Gray
                    )
                )
                
                OutlinedTextField(
                    value = balance,
                    onValueChange = { balance = it },
                    label = { Text("Current Balance / Value") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF7C3AED),
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color(0xFF7C3AED),
                        unfocusedLabelColor = Color.Gray
                    )
                )

                if (typeItem.category == "BANKING" || typeItem.category == "CREDIT") {
                    OutlinedTextField(
                        value = institution,
                        onValueChange = { institution = it },
                        label = { Text("Institution (e.g. HDFC)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF7C3AED),
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = Color(0xFF7C3AED),
                            unfocusedLabelColor = Color.Gray
                        )
                    )
                    OutlinedTextField(
                        value = last4,
                        onValueChange = { last4 = it },
                        label = { Text("Last 4 Digits") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF7C3AED),
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = Color(0xFF7C3AED),
                            unfocusedLabelColor = Color.Gray
                        )
                    )
                }

                if (typeItem.category == "CREDIT") {
                    OutlinedTextField(
                        value = limit,
                        onValueChange = { limit = it },
                        label = { Text("Credit Limit") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF7C3AED),
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = Color(0xFF7C3AED),
                            unfocusedLabelColor = Color.Gray
                        )
                    )
                }

                if (typeItem.category == "DEPOSITS") {
                    OutlinedTextField(
                        value = interestRate,
                        onValueChange = { interestRate = it },
                        label = { Text("Interest Rate (%)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF7C3AED),
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = Color(0xFF7C3AED),
                            unfocusedLabelColor = Color.Gray
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    onConfirm(
                        name, 
                        balance.toDoubleOrNull() ?: 0.0, 
                        institution.ifBlank { null },
                        last4.ifBlank { null },
                        limit.toDoubleOrNull(),
                        interestRate.toDoubleOrNull(),
                        null // Maturity Date
                    ) 
                },
                enabled = name.isNotBlank() && balance.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = RupeeFlowTheme.colors.income)
            ) {
                Text("Add", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        }
    )
}

@Composable
fun DeleteConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF181818),
        title = { Text(title, color = Color.White) },
        text = { Text(message, color = Color(0xFF9CA3AF)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = RupeeFlowTheme.colors.expense)
            ) {
                Text("Close Account")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferDialog(
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onTransfer: (fromAccountId: Long, toAccountId: Long, amount: Double, note: String, timestamp: Long) -> Unit,
    title: String = "Move Money",
    initialFromAccountId: Long? = null,
    initialToAccountId: Long? = null,
    initialAmount: String = "",
    initialNote: String = "",
    initialTimestamp: Long? = null
) {
    var fromAccountId by remember { mutableStateOf(initialFromAccountId ?: accounts.firstOrNull()?.id ?: 0L) }
    var toAccountId by remember { mutableStateOf(initialToAccountId ?: accounts.getOrNull(1)?.id ?: accounts.firstOrNull()?.id ?: 0L) }
    var amount by remember { mutableStateOf(initialAmount) }
    var note by remember { mutableStateOf(initialNote) }
    var selectedTimestamp by remember { mutableStateOf(initialTimestamp ?: System.currentTimeMillis()) }
    var showFromDialog by remember { mutableStateOf(false) }
    var showToDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF181818),
        title = { Text(title, color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TransferAccountSelector("From", accounts.find { it.id == fromAccountId }?.name ?: "Select Account", { showFromDialog = true })
                TransferAccountSelector("To", accounts.find { it.id == toAccountId }?.name ?: "Select Account", { showToDialog = true })

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = RupeeFlowTheme.colors.income
                    )
                )

                // Date & Time Selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedCard(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.outlinedCardColors(containerColor = Color(0xFF1E1E1E))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Event, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = dateFormat.format(Date(selectedTimestamp)), style = MaterialTheme.typography.bodySmall, color = Color.White)
                        }
                    }

                    OutlinedCard(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.outlinedCardColors(containerColor = Color(0xFF1E1E1E))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Schedule, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = timeFormat.format(Date(selectedTimestamp)), style = MaterialTheme.typography.bodySmall, color = Color.White)
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onTransfer(fromAccountId, toAccountId, amount.toDoubleOrNull() ?: 0.0, note, selectedTimestamp) },
                enabled = amount.isNotEmpty() && fromAccountId != toAccountId,
                colors = ButtonDefaults.buttonColors(containerColor = RupeeFlowTheme.colors.income)
            ) {
                Text("Transfer", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedTimestamp)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val calendar = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
                        val timePart = calendar.get(Calendar.HOUR_OF_DAY) * 3600000L +
                                       calendar.get(Calendar.MINUTE) * 60000L +
                                       calendar.get(Calendar.SECOND) * 1000L +
                                       calendar.get(Calendar.MILLISECOND)
                        selectedTimestamp = it + timePart
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
                    selectedTimestamp = cal.timeInMillis
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

    if (showFromDialog) {
        AccountSelectionDialog(
            accounts = accounts,
            onDismiss = { showFromDialog = false },
            onAccountSelected = { account ->
                fromAccountId = account.id
                showFromDialog = false
            }
        )
    }

    if (showToDialog) {
        AccountSelectionDialog(
            accounts = accounts,
            onDismiss = { showToDialog = false },
            onAccountSelected = { account ->
                toAccountId = account.id
                showToDialog = false
            }
        )
    }
}

@Composable
fun AccountSelectionDialog(
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onAccountSelected: (Account) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF181818),
        title = { Text("Select Account", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (accounts.isEmpty()) {
                    Text("No accounts found. Please add an account first.", color = Color.Gray)
                }
                accounts.forEach { account ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAccountSelected(account) },
                        color = Color(0xFF1F2937),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(account.name, color = Color.White)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ReconcileBalanceDialog(
    account: Account,
    preferences: UserPreferences,
    onDismiss: () -> Unit,
    onConfirm: (Double, String?, String?) -> Unit
) {
    var actualBalanceStr by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val actualBalance = actualBalanceStr.toDoubleOrNull() ?: account.balance
    val difference = actualBalance - account.balance

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF181818),
        title = {
            Text("Reconcile Balance", color = Color.White, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Account", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                        Text(account.name, color = Color.White, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Recorded balance", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                        Text(CurrencyFormatter.format(account.balance, preferences), color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                OutlinedTextField(
                    value = actualBalanceStr,
                    onValueChange = { 
                        if (it.isEmpty() || it.toDoubleOrNull() != null || (it == "-" && account.category == "LIABILITIES")) {
                            actualBalanceStr = it 
                        }
                    },
                    label = { Text("Actual balance") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF7C3AED),
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                        focusedLabelColor = Color(0xFF7C3AED),
                        unfocusedLabelColor = Color.Gray
                    )
                )

                if (actualBalanceStr.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Difference", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = (if (difference > 0) "+" else "") + CurrencyFormatter.format(difference, preferences),
                            color = when {
                                difference > 0 -> RupeeFlowTheme.colors.income
                                difference < 0 -> RupeeFlowTheme.colors.expense
                                else -> Color.White
                            },
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF7C3AED),
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                        focusedLabelColor = Color(0xFF7C3AED),
                        unfocusedLabelColor = Color.Gray
                    )
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF7C3AED),
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                        focusedLabelColor = Color(0xFF7C3AED),
                        unfocusedLabelColor = Color.Gray
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(actualBalance, reason.ifBlank { null }, note.ifBlank { null }) },
                enabled = actualBalanceStr.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Confirm", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}

@Composable
fun EditAccountDialog(
    account: Account,
    viewModel: AccountViewModel = hiltViewModel(),
    onDismiss: () -> Unit,
    onConfirm: (Account) -> Unit,
    onCloseAccount: () -> Unit
) {
    var name by remember { mutableStateOf(account.name) }
    var institution by remember { mutableStateOf(account.institutionName ?: "") }
    var last4 by remember { mutableStateOf(account.accountNumberLast4 ?: "") }
    var limit by remember { mutableStateOf(account.creditLimit?.toString() ?: "") }
    var interestRate by remember { mutableStateOf(account.interestRate?.toString() ?: "") }

    var showAddDebitCardDialog by remember { mutableStateOf(false) }
    var showAddUpiAppDialog by remember { mutableStateOf(false) }

    val debitCards by viewModel.getDebitCardsForAccount(account.id).collectAsState(initial = emptyList())
    val upiApps by viewModel.getUpiAppsForAccount(account.id).collectAsState(initial = emptyList())

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF181818),
        title = { Text("Edit Account", color = Color.White) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF7C3AED),
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color(0xFF7C3AED),
                        unfocusedLabelColor = Color.Gray
                    )
                )

                if (account.category == "BANKING" || account.category == "CREDIT") {
                    OutlinedTextField(
                        value = institution,
                        onValueChange = { institution = it },
                        label = { Text("Institution") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF7C3AED),
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = Color(0xFF7C3AED),
                            unfocusedLabelColor = Color.Gray
                        )
                    )
                    OutlinedTextField(
                        value = last4,
                        onValueChange = { last4 = it },
                        label = { Text("Last 4 Digits") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF7C3AED),
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = Color(0xFF7C3AED),
                            unfocusedLabelColor = Color.Gray
                        )
                    )
                }

                if (account.category == "BANKING") {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    
                    Text(
                        "Payment Methods",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )

                    // Debit Cards Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Debit Cards", style = MaterialTheme.typography.labelLarge, color = Color(0xFF9CA3AF))
                        TextButton(onClick = { showAddDebitCardDialog = true }) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Card")
                        }
                    }

                    if (debitCards.isEmpty()) {
                        Text("No saved debit cards", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    } else {
                        debitCards.forEach { card ->
                            Surface(
                                color = Color(0xFF1E1E1E),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(card.cardName, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                        Text("•••• ${card.last4Digits} ${card.network ?: ""}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    }
                                    IconButton(onClick = { viewModel.deleteDebitCard(card) }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = RupeeFlowTheme.colors.expense)
                                    }
                                }
                            }
                        }
                    }

                    // UPI Apps Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("UPI Apps", style = MaterialTheme.typography.labelLarge, color = Color(0xFF9CA3AF))
                        TextButton(onClick = { showAddUpiAppDialog = true }) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add App")
                        }
                    }

                    val customUpiApps = upiApps.filter { it.isCustom }
                    if (customUpiApps.isEmpty()) {
                        Text("No custom UPI apps added", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    } else {
                        customUpiApps.forEach { app ->
                            Surface(
                                color = Color(0xFF1E1E1E),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(app.appName, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                    IconButton(onClick = { viewModel.deleteUpiApp(app) }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = RupeeFlowTheme.colors.expense)
                                    }
                                }
                            }
                        }
                    }
                }

                if (account.category == "CREDIT") {
                    OutlinedTextField(
                        value = limit,
                        onValueChange = { limit = it },
                        label = { Text("Credit Limit") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF7C3AED),
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = Color(0xFF7C3AED),
                            unfocusedLabelColor = Color.Gray
                        )
                    )
                }

                if (account.category == "DEPOSITS") {
                    OutlinedTextField(
                        value = interestRate,
                        onValueChange = { interestRate = it },
                        label = { Text("Interest Rate (%)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF7C3AED),
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = Color(0xFF7C3AED),
                            unfocusedLabelColor = Color.Gray
                        )
                    )
                }

                TextButton(
                    onClick = onCloseAccount,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = null, tint = RupeeFlowTheme.colors.expense, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Close Account", color = RupeeFlowTheme.colors.expense)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        account.copy(
                            name = name,
                            institutionName = institution.ifBlank { null },
                            accountNumberLast4 = last4.ifBlank { null },
                            creditLimit = limit.toDoubleOrNull(),
                            interestRate = interestRate.toDoubleOrNull()
                        )
                    )
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = RupeeFlowTheme.colors.income)
            ) {
                Text("Save", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        }
    )

    if (showAddDebitCardDialog) {
        AddEditDebitCardDialog(
            accountId = account.id,
            onDismiss = { showAddDebitCardDialog = false },
            onConfirm = { debitCard ->
                viewModel.addDebitCard(debitCard)
                showAddDebitCardDialog = false
            }
        )
    }

    if (showAddUpiAppDialog) {
        AddEditCustomUpiAppDialog(
            accountId = account.id,
            onDismiss = { showAddUpiAppDialog = false },
            onConfirm = { upiApp ->
                viewModel.addUpiApp(upiApp)
                showAddUpiAppDialog = false
            }
        )
    }
}

@Composable
fun TransferAccountSelector(label: String, value: String, onClick: () -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
        Surface(
            modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 8.dp),
            color = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(value, modifier = Modifier.padding(16.dp), color = Color.White)
        }
    }
}
