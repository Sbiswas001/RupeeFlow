package sayan.apps.rupeeflow.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DateFormatSymbols
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthPickerDialog(
    initialMonth: Calendar,
    onDismiss: () -> Unit,
    onConfirm: (Calendar) -> Unit
) {
    var selectedYear by remember { mutableStateOf(initialMonth.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(initialMonth.get(Calendar.MONTH)) }
    
    val months = DateFormatSymbols().shortMonths.filter { it.isNotEmpty() }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val result = Calendar.getInstance().apply {
                    set(Calendar.YEAR, selectedYear)
                    set(Calendar.MONTH, selectedMonth)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                onConfirm(result)
            }) {
                Text("Select", color = Color(0xFF7C3AED))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedYear-- }) {
                    Icon(Icons.Rounded.ChevronLeft, contentDescription = null)
                }
                Text(
                    text = selectedYear.toString(),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = { selectedYear++ }) {
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                }
            }
        },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.height(240.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(months.indices.toList()) { index ->
                    val isSelected = index == selectedMonth
                    Box(
                        modifier = Modifier
                            .aspectRatio(1.5f)
                            .background(
                                color = if (isSelected) Color(0xFF7C3AED) else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedMonth = index },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = months[index],
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                }
            }
        },
        containerColor = Color(0xFF111827),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}
