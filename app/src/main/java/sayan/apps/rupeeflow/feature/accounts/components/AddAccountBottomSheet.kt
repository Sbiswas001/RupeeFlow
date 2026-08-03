package sayan.apps.rupeeflow.feature.accounts.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountBottomSheet(
    onDismiss: () -> Unit,
    onSelectType: (AccountTypeItem) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF181818),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.1f)) }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
        ) {
            Text(
                text = "Add Account",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(accountTypeItems) { item ->
                    AccountTypeOption(item) {
                        onSelectType(item)
                    }
                }
            }
        }
    }
}

@Composable
fun AccountTypeOption(item: AccountTypeItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E1E1E),
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(item.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF9CA3AF),
            maxLines = 1
        )
    }
}

data class AccountTypeItem(
    val label: String,
    val category: String,
    val subType: String,
    val icon: ImageVector
)

val accountTypeItems = listOf(
    AccountTypeItem("Bank", "BANKING", "SAVINGS", Icons.Rounded.AccountBalance),
    AccountTypeItem("Cash", "CASH_WALLETS", "CASH", Icons.Rounded.Payments),
    AccountTypeItem("Wallet", "CASH_WALLETS", "WALLET", Icons.Rounded.AccountBalanceWallet),
    AccountTypeItem("Credit Card", "CREDIT", "CREDIT_CARD", Icons.Rounded.CreditCard),
    AccountTypeItem("FD", "DEPOSITS", "FD", Icons.Rounded.AccountBalance),
    AccountTypeItem("RD", "DEPOSITS", "RD", Icons.Rounded.Sync),
    AccountTypeItem("Mutual Fund", "INVESTMENTS", "MUTUAL_FUND", Icons.Rounded.ShowChart),
    AccountTypeItem("Stocks", "INVESTMENTS", "STOCKS", Icons.Rounded.BarChart),
    AccountTypeItem("Gold", "INVESTMENTS", "GOLD", Icons.Rounded.MilitaryTech),
    AccountTypeItem("Loan", "LIABILITIES", "LOAN", Icons.Rounded.Home),
    AccountTypeItem("Property", "ASSETS", "ASSET", Icons.Rounded.Apartment),
    AccountTypeItem("Other", "OTHERS", "OTHERS", Icons.Rounded.Category)
)
