package sayan.apps.rupeeflow.feature.accounts.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import sayan.apps.rupeeflow.domain.model.SavedUpiApp

@Composable
fun AddEditCustomUpiAppDialog(
    accountId: Long?,
    upiAppToEdit: SavedUpiApp? = null,
    onDismiss: () -> Unit,
    onConfirm: (SavedUpiApp) -> Unit
) {
    var appName by remember { mutableStateOf(upiAppToEdit?.appName ?: "") }
    var appPackage by remember { mutableStateOf(upiAppToEdit?.appPackage ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (upiAppToEdit == null) "Add Custom UPI App" else "Edit UPI App",
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
                    value = appName,
                    onValueChange = { appName = it },
                    label = { Text("App Name (e.g. CRED)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = appPackage,
                    onValueChange = { appPackage = it },
                    label = { Text("Package Identifier (Optional)") },
                    placeholder = { Text("e.g. com.cred.app") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val app = SavedUpiApp(
                        id = upiAppToEdit?.id ?: 0,
                        accountId = accountId,
                        appName = appName.trim(),
                        appPackage = appPackage.ifBlank { null },
                        isCustom = true,
                        isDefault = false
                    )
                    onConfirm(app)
                },
                enabled = appName.isNotBlank()
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
