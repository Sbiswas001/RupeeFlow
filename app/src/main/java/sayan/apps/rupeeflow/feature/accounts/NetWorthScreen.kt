package sayan.apps.rupeeflow.feature.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import sayan.apps.rupeeflow.core.designsystem.components.LineChart
import sayan.apps.rupeeflow.core.designsystem.components.LineChartPoint
import sayan.apps.rupeeflow.core.util.CurrencyFormatter
import sayan.apps.rupeeflow.core.util.LocalUserPreferences
import sayan.apps.rupeeflow.domain.model.UserPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetWorthScreen(
    onNavigateBack: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val preferences = LocalUserPreferences.current
    val accounts by viewModel.accounts.collectAsState()
    
    val assets = accounts.filter { it.category != "LIABILITIES" }.sumOf { it.balance }
    val liabilities = accounts.filter { it.category == "LIABILITIES" }.sumOf { it.balance }
    val netWorth = assets - liabilities

    Scaffold(
        containerColor = Color.Black
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Text(
                    text = CurrencyFormatter.format(netWorth, preferences),
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "Total Wealth",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF9CA3AF)
                )
            }

            item {
                Surface(
                    color = Color(0xFF181818),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("Growth", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Spacer(modifier = Modifier.height(24.dp))
                        LineChart(
                            points = listOf(
                                LineChartPoint(1f, netWorth * 0.8),
                                LineChartPoint(2f, netWorth * 0.85),
                                LineChartPoint(3f, netWorth * 0.9),
                                LineChartPoint(4f, netWorth)
                            ),
                            modifier = Modifier.height(200.dp),
                            lineColor = Color(0xFF10B981)
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    NetWorthStatCard("Assets", assets, Color(0xFF10B981), Modifier.weight(1f), preferences)
                    NetWorthStatCard("Liabilities", liabilities, Color(0xFFEF4444), Modifier.weight(1f), preferences)
                }
            }
        }
    }
}

@Composable
fun NetWorthStatCard(label: String, amount: Double, color: Color, modifier: Modifier = Modifier, preferences: UserPreferences) {
    Surface(
        modifier = modifier,
        color = Color(0xFF181818),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
            Spacer(modifier = Modifier.height(8.dp))
            Text(CurrencyFormatter.format(amount, preferences), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = color)
        }
    }
}
