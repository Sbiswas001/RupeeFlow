package sayan.apps.rupeeflow.feature.accounts.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import sayan.apps.rupeeflow.domain.model.DebitCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditDebitCardDialog(
    accountId: Long,
    debitCardToEdit: DebitCard? = null,
    onDismiss: () -> Unit,
    onConfirm: (DebitCard) -> Unit
) {
    var cardName by remember { mutableStateOf(debitCardToEdit?.cardName ?: "") }
    var last4Digits by remember { mutableStateOf(debitCardToEdit?.last4Digits ?: "") }
    var selectedNetwork by remember { mutableStateOf(debitCardToEdit?.network ?: "Visa") }
    var nickname by remember { mutableStateOf(debitCardToEdit?.nickname ?: "") }
    var networkExpanded by remember { mutableStateOf(false) }

    val networks = listOf("Visa", "Mastercard", "RuPay", "Amex", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (debitCardToEdit == null) "Add Debit Card" else "Edit Debit Card",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = cardName,
                    onValueChange = { cardName = it },
                    label = { Text("Card Name (e.g. SBI Debit Card)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = last4Digits,
                    onValueChange = {
                        if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                            last4Digits = it
                        }
                    },
                    label = { Text("Last 4 Digits") },
                    placeholder = { Text("4821") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = networkExpanded,
                    onExpandedChange = { networkExpanded = !networkExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedNetwork,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Card Network") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = networkExpanded) },
                        modifier = Modifier
                            .menuAnchor(type = MenuAnchorType.PrimaryEditable, enabled = true)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = networkExpanded,
                        onDismissRequest = { networkExpanded = false }
                    ) {
                        networks.forEach { network ->
                            DropdownMenuItem(
                                text = { Text(network) },
                                onClick = {
                                    selectedNetwork = network
                                    networkExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("Nickname (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val card = DebitCard(
                        id = debitCardToEdit?.id ?: 0,
                        accountId = accountId,
                        cardName = cardName.ifBlank { "Debit Card" },
                        last4Digits = last4Digits.ifBlank { "0000" },
                        network = selectedNetwork.ifBlank { null },
                        nickname = nickname.ifBlank { null }
                    )
                    onConfirm(card)
                },
                enabled = cardName.isNotBlank() && last4Digits.length == 4
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
