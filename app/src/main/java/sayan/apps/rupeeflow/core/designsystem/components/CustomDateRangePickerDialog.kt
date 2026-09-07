package sayan.apps.rupeeflow.core.designsystem.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import sayan.apps.rupeeflow.core.designsystem.theme.RupeeFlowTheme
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDateRangePickerDialog(
    initialStartMillis: Long? = null,
    initialEndMillis: Long? = null,
    onDismiss: () -> Unit,
    onConfirm: (startMillis: Long, endMillis: Long) -> Unit
) {
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStartMillis ?: System.currentTimeMillis(),
        initialSelectedEndDateMillis = initialEndMillis ?: System.currentTimeMillis()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val start = dateRangePickerState.selectedStartDateMillis ?: System.currentTimeMillis()
                    val end = dateRangePickerState.selectedEndDateMillis ?: start
                    
                    val startCal = Calendar.getInstance().apply {
                        timeInMillis = start
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    
                    val endCal = Calendar.getInstance().apply {
                        timeInMillis = end
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }

                    onConfirm(startCal.timeInMillis, endCal.timeInMillis)
                },
                enabled = dateRangePickerState.selectedStartDateMillis != null
            ) {
                Text("Apply", color = RupeeFlowTheme.colors.income)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        },
        colors = DatePickerDefaults.colors(
            containerColor = Color(0xFF181818)
        )
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            modifier = Modifier
                .height(500.dp)
                .fillMaxWidth(),
            title = {
                Text(
                    text = "Select Date Range",
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            },
            headline = {
                val formatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
                val start = dateRangePickerState.selectedStartDateMillis
                val end = dateRangePickerState.selectedEndDateMillis
                
                val text = when {
                    start != null && end != null -> "${formatter.format(Date(start))} – ${formatter.format(Date(end))}"
                    start != null -> formatter.format(Date(start))
                    else -> "Select start and end dates"
                }

                Text(
                    text = text,
                    modifier = Modifier.padding(start = 24.dp, bottom = 12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF9CA3AF)
                )
            },
            showModeToggle = false,
            colors = DatePickerDefaults.colors(
                containerColor = Color(0xFF181818),
                titleContentColor = Color.White,
                headlineContentColor = Color.White,
                weekdayContentColor = Color(0xFF9CA3AF),
                subheadContentColor = Color.White,
                yearContentColor = Color.White,
                currentYearContentColor = RupeeFlowTheme.colors.income,
                selectedYearContentColor = Color.Black,
                selectedYearContainerColor = RupeeFlowTheme.colors.income,
                dayContentColor = Color.White,
                disabledDayContentColor = Color(0xFF4B5563),
                selectedDayContentColor = Color.Black,
                selectedDayContainerColor = RupeeFlowTheme.colors.income,
                todayContentColor = RupeeFlowTheme.colors.income,
                todayDateBorderColor = RupeeFlowTheme.colors.income,
                dayInSelectionRangeContentColor = Color.White,
                dayInSelectionRangeContainerColor = RupeeFlowTheme.colors.income.copy(alpha = 0.2f)
            )
        )
    }
}
