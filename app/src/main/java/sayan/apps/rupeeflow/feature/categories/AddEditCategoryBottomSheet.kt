package sayan.apps.rupeeflow.feature.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sayan.apps.rupeeflow.domain.model.Category
import sayan.apps.rupeeflow.domain.model.TransactionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCategoryBottomSheet(
    category: Category? = null,
    onDismiss: () -> Unit,
    onConfirm: (Category) -> Unit
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var type by remember { mutableStateOf(category?.type ?: TransactionType.EXPENSE) }
    var icon by remember { mutableStateOf(category?.icon ?: "🍔") }
    var colorHex by remember { mutableStateOf(category?.colorHex ?: "#EF4444") }
    var budget by remember { mutableStateOf(category?.budget?.toString() ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF111827),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = if (category == null) "New Category" else "Edit Category",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Category Name") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1F2937),
                    unfocusedContainerColor = Color(0xFF1F2937),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = type == TransactionType.EXPENSE,
                    onClick = { type = TransactionType.EXPENSE },
                    label = { Text("Expense") },
                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                )
                FilterChip(
                    selected = type == TransactionType.INCOME,
                    onClick = { type = TransactionType.INCOME },
                    label = { Text("Income") },
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Select Icon", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
            EmojiGrid(selectedEmoji = icon, onEmojiSelected = { icon = it })
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Select Color", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
            ColorGrid(selectedColor = colorHex, onColorSelected = { colorHex = it })
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    onConfirm(
                        Category(
                            id = category?.id ?: 0,
                            name = name,
                            icon = icon,
                            colorHex = colorHex,
                            type = type,
                            budget = budget.toDoubleOrNull()
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
            ) {
                Text("Save Category")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun EmojiGrid(selectedEmoji: String, onEmojiSelected: (String) -> Unit) {
    val emojis = listOf("🍔", "🛒", "🚕", "⛽", "🏠", "⚡", "📱", "🎬", "🛍", "🏥", "🎓", "✈", "🎁", "💼", "🧾", "📦")
    LazyVerticalGrid(
        columns = GridCells.Adaptive(48.dp),
        modifier = Modifier.height(120.dp)
    ) {
        items(emojis) { emoji ->
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(if (selectedEmoji == emoji) Color(0xFF1F2937) else Color.Transparent, CircleShape)
                    .clickable { onEmojiSelected(emoji) },
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 24.sp)
            }
        }
    }
}

@Composable
fun ColorGrid(selectedColor: String, onColorSelected: (String) -> Unit) {
    val colors = listOf("#EF4444", "#F59E0B", "#10B981", "#3B82F6", "#6366F1", "#8B5CF6", "#EC4899", "#6B7280")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        colors.forEach { colorStr ->
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(android.graphics.Color.parseColor(colorStr)), CircleShape)
                    .clickable { onColorSelected(colorStr) },
                contentAlignment = Alignment.Center
            ) {
                if (selectedColor == colorStr) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
