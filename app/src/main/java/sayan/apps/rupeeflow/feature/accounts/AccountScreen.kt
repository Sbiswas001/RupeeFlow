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
import androidx.hilt.navigation.compose.hiltViewModel
import sayan.apps.rupeeflow.core.util.CurrencyFormatter
import sayan.apps.rupeeflow.core.util.LocalUserPreferences
import sayan.apps.rupeeflow.domain.model.Account
import sayan.apps.rupeeflow.domain.model.UserPreferences
import androidx.compose.ui.draw.rotate
import sayan.apps.rupeeflow.feature.accounts.components.AddAccountBottomSheet
import sayan.apps.rupeeflow.feature.accounts.components.AccountTypeItem
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
    var accountToDelete by remember { mutableStateOf<Account?>(null) }
    var selectedCategory by remember { mutableStateOf("All") }
    var isFabExpanded by remember { mutableStateOf(false) }

    val categories = listOf("All", "Banks", "Investments", "Credit", "Cash", "Liabilities")
    
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
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                // Header
                item {
                    Text(
                        text = "Portfolio",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-1).sp
                        ),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
                        color = Color.White
                    )
                }

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
                                    selectedContainerColor = Color(0xFF10B981),
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
                    accounts.filter { it.category.contains(selectedCategory.uppercase().replace(" ", "_")) }
                }
                
                val groupedAccounts = filteredAccounts.groupBy { it.category }
                
                groupedAccounts.forEach { (category, accountList) ->
                    item {
                        Text(
                            text = category.replace("_", " ").lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 24.dp),
                            color = Color(0xFF9CA3AF)
                        )
                    }
                    items(accountList) { account ->
                        AccountItem(
                            account = account,
                            preferences = preferences,
                            onClick = { /* Detail */ },
                            onLongClick = { accountToDelete = account }
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
                onTransfer = { from, to, amount, note ->
                    viewModel.transferFunds(from, to, amount, note.ifBlank { null })
                    showTransferDialog = false
                }
            )
        }

        accountToDelete?.let { account ->
            DeleteConfirmationDialog(
                title = "Delete Account",
                message = "Are you sure you want to delete '${account.name}'?",
                onConfirm = {
                    viewModel.deleteAccount(account)
                    accountToDelete = null
                },
                onDismiss = { accountToDelete = null }
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
                    Icon(Icons.AutoMirrored.Rounded.TrendingUp, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                    Text(
                        text = CurrencyFormatter.format(5240.0, preferences), 
                        style = MaterialTheme.typography.labelMedium, 
                        color = Color(0xFF10B981)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "$accountCount Accounts tracked",
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
                    text = "${account.institutionName ?: ""} ${account.accountNumberLast4?.let { "•••• $it" } ?: ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF9CA3AF)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyFormatter.format(account.balance, preferences),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (account.category == "LIABILITIES") Color(0xFFEF4444) else Color.White
                )
                Text("Today", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF).copy(alpha = 0.5f))
            }
        }
    }
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
    else -> Icons.Rounded.Category
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
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = balance,
                    onValueChange = { balance = it },
                    label = { Text("Current Balance / Value") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (typeItem.category == "BANKING" || typeItem.category == "CREDIT") {
                    OutlinedTextField(
                        value = institution,
                        onValueChange = { institution = it },
                        label = { Text("Institution (e.g. HDFC)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = last4,
                        onValueChange = { last4 = it },
                        label = { Text("Last 4 Digits") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (typeItem.category == "CREDIT") {
                    OutlinedTextField(
                        value = limit,
                        onValueChange = { limit = it },
                        label = { Text("Credit Limit") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (typeItem.category == "DEPOSITS") {
                    OutlinedTextField(
                        value = interestRate,
                        onValueChange = { interestRate = it },
                        label = { Text("Interest Rate (%)") },
                        modifier = Modifier.fillMaxWidth()
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
            ) {
                Text("Delete")
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
    onTransfer: (Long, Long, Double, String) -> Unit
) {
    var fromAccountId by remember { mutableStateOf(accounts.first().id) }
    var toAccountId by remember { mutableStateOf(accounts.getOrNull(1)?.id ?: accounts.first().id) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF181818),
        title = { Text("Move Money", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Simplified dropdowns for dark mode
                TransferAccountSelector("From", accounts.find { it.id == fromAccountId }?.name ?: "", { fromExpanded = true })
                TransferAccountSelector("To", accounts.find { it.id == toAccountId }?.name ?: "", { toExpanded = true })

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF10B981)
                    )
                )

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
                onClick = { onTransfer(fromAccountId, toAccountId, amount.toDoubleOrNull() ?: 0.0, note) },
                enabled = amount.isNotEmpty() && fromAccountId != toAccountId,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
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
