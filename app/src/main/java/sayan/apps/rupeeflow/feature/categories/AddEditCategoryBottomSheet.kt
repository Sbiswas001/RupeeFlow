package sayan.apps.rupeeflow.feature.categories

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
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

val CATEGORY_COLORS = listOf(
    "#EF4444", "#F59E0B", "#10B981", "#3B82F6", "#6366F1", "#8B5CF6", "#EC4899", "#6B7280",
    "#F97316", "#EAB308", "#14B8A6", "#06B6D4", "#0EA5E9", "#A855F7", "#D946EF", "#F43F5E",
    "#84CC16", "#22C55E", "#64748B", "#71717A", "#737373", "#78716C", "#F87171", "#FBBF24",
    "#34D399", "#60A5FA", "#818CF8", "#A78BFA", "#F472B6", "#FB923C", "#2DD4BF", "#22D3EE"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCategoryBottomSheet(
    category: Category? = null,
    defaultType: TransactionType = TransactionType.EXPENSE,
    existingCategories: List<Category> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (Category) -> Unit,
    onDelete: (Category) -> Unit = {}
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var type by remember { mutableStateOf(category?.type ?: defaultType) }
    var icon by remember { mutableStateOf(category?.icon ?: "🍔") }
    val colorHex = remember {
        category?.colorHex ?: run {
            val usedColors = existingCategories.map { it.colorHex }
            val availableColors = CATEGORY_COLORS.filter { it !in usedColors }
            if (availableColors.isNotEmpty()) availableColors.random() else CATEGORY_COLORS.random()
        }
    }
    var budget by remember { mutableStateOf(category?.budget?.toString() ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF111827),
        contentColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (category == null || category.id == 0L) "New Category" else "Edit Category",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                
                if (category != null && category.id != 0L) {
                    IconButton(onClick = { 
                        onDelete(category)
                        onDismiss()
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFEF4444)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text("Category Name", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Category Name", color = Color.Gray) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1F2937),
                    unfocusedContainerColor = Color(0xFF1F2937),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { type = TransactionType.EXPENSE },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (type == TransactionType.EXPENSE) Color(0xFF1F2937) else Color(0xFF1F2937).copy(alpha = 0.5f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = if (type == TransactionType.EXPENSE) BorderStroke(1.dp, Color(0xFF7C3AED)) else null
                ) {
                    Text("Expense", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = { type = TransactionType.INCOME },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (type == TransactionType.INCOME) Color(0xFF1F2937) else Color(0xFF1F2937).copy(alpha = 0.5f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = if (type == TransactionType.INCOME) BorderStroke(1.dp, Color(0xFF7C3AED)) else null
                ) {
                    Text("Income", fontWeight = FontWeight.SemiBold)
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text("Select Icon", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            EmojiGrid(selectedEmoji = icon, onEmojiSelected = { icon = it })
            
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
                            budget = if (type == TransactionType.EXPENSE) budget.toDoubleOrNull() else null
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7C3AED),
                    disabledContainerColor = Color(0xFF7C3AED).copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(26.dp)
            ) {
                Text(if (category == null) "Save Category" else "Save Changes", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun EmojiGrid(selectedEmoji: String, onEmojiSelected: (String) -> Unit) {
    val emojis = listOf(
        // Food & Dining
        "🍔", "🍕", "☕", "🍺", "🍷", "🍰", "🍎", "🥗", "🍜", "🍣",
        // Shopping & Groceries
        "🛒", "🛍", "👗", "👟", "💍", "💄", "💻", "⌚", "🎧", "📷",
        // Transport & Travel
        "🚕", "⛽", "✈", "🚆", "🚲", "🚀", "🚗", "🛵", "🎫", "🏨",
        // Bills & Utilities
        "⚡", "💡", "💧", "🔥", "📶", "🏠", "🔑", "🛠", "📦", "📫",
        // Entertainment & Leisure
        "🎬", "🎮", "🎸", "🎨", "🎟", "🎳", "⚽", "🏀", "🏆", "🎯",
        // Health & Fitness
        "🏥", "💊", "🩺", "💪", "🧘", "🏃", "🏋", "🦷", "🧴", "🧼",
        // Education & Work
        "🎓", "📚", "💼", "📝", "📈", "📉", "💰", "💳", "🧾", "📌",
        // Personal, Family & Pets
        "🎁", "👶", "🐾", "🐶", "🐱", "❤️", "👑", "⭐", "🎉", "💸"
    )
    LazyVerticalGrid(
        columns = GridCells.Adaptive(44.dp),
        modifier = Modifier.height(180.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(emojis) { emoji ->
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(if (selectedEmoji == emoji) Color(0xFF1F2937) else Color.Transparent, CircleShape)
                    .border(if (selectedEmoji == emoji) BorderStroke(1.dp, Color(0xFF7C3AED)) else BorderStroke(0.dp, Color.Transparent), CircleShape)
                    .clickable { onEmojiSelected(emoji) },
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 22.sp)
            }
        }
    }
}
