package sayan.apps.rupeeflow.feature.insights

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sayan.apps.rupeeflow.feature.analytics.AnalyticsScreen
import sayan.apps.rupeeflow.feature.planning.PlanningScreen
import sayan.apps.rupeeflow.feature.reports.ReportsScreen

@Composable
fun InsightsScreen(
    onNavigateToAddRecurring: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Analytics", "Planning", "Reports")
    val icons = listOf(Icons.Rounded.Assessment, Icons.Rounded.EventNote, Icons.Rounded.Description)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = "Insights",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1).sp
                    ),
                    color = Color.White
                )
                Text(
                    text = "Understand your money better",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF9CA3AF)
                )
            }
            
            // Date Selector
            Surface(
                color = Color(0xFF181818),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.clickable { /* Date Picker */ }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.CalendarToday, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Jul 2026", style = MaterialTheme.typography.labelLarge, color = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = Color.White)
                }
            }
        }

        // Samsung One UI Style Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTabIndex == index
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTabIndex = index }
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = icons[index],
                            contentDescription = null,
                            tint = if (isSelected) Color(0xFF10B981) else Color(0xFF9CA3AF),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) Color(0xFF10B981) else Color(0xFF9CA3AF)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Thick indicator
                    AnimatedVisibility(
                        visible = isSelected,
                        enter = expandHorizontally() + fadeIn(),
                        exit = shrinkHorizontally() + fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(4.dp)
                                .background(Color(0xFF10B981), CircleShape)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTabIndex) {
                0 -> AnalyticsScreen(showTitle = false)
                1 -> PlanningScreen(
                    onNavigateToAddRecurring = onNavigateToAddRecurring,
                    showTitle = false
                )
                2 -> ReportsScreen()
            }
        }
    }
}
