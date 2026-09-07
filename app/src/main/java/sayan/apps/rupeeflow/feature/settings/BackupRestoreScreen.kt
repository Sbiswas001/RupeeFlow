package sayan.apps.rupeeflow.feature.settings

import androidx.activity.result.IntentSenderRequest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.google.android.gms.auth.api.identity.Identity
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.launch
import sayan.apps.rupeeflow.core.util.LocalUserPreferences
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val preferences by viewModel.userPreferences.collectAsState()
    val dateFormat = SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault())
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var showRestoreSuccessDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showMoreOptions by remember { mutableStateOf(false) }

    val isGoogleSignedIn by viewModel.isGoogleSignedIn.collectAsState()
    val googleAccountEmail by viewModel.googleAccountEmail.collectAsState()
    val driveBackupTimestamp by viewModel.driveBackupTimestamp.collectAsState()

    // Status indicator logic
    val isBackupUpToDate = remember(driveBackupTimestamp) {
        driveBackupTimestamp?.let { 
            val diff = System.currentTimeMillis() - it
            diff < 24 * 60 * 60 * 1000 // Up to date if less than 24 hours old
        } ?: false
    }

    // To store what action to take after authorization
    var pendingDriveAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val authorizationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                scope.launch {
                    try {
                        val authResult = Identity.getAuthorizationClient(context)
                            .getAuthorizationResultFromIntent(result.data)
                        val token = authResult.accessToken
                        if (token != null) {
                            viewModel.fetchDriveBackupInfo(token)
                            pendingDriveAction?.invoke()
                        } else {
                            snackbarHostState.showSnackbar("Failed to get access token")
                        }
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Authorization failed: ${e.message}")
                    } finally {
                        pendingDriveAction = null
                    }
                }
            } else {
                pendingDriveAction = null
            }
        }
    )

    fun performWithDriveAccess(action: (String) -> Unit) {
        val requestedScopes = listOf(DriveScopes.DRIVE_APPDATA)
        val authClient = Identity.getAuthorizationClient(context)
        val authRequest = viewModel.authorizationHelper.getAuthorizationRequest(requestedScopes)
        
        authClient.authorize(authRequest)
            .addOnSuccessListener { result ->
                if (result.hasResolution()) {
                    pendingDriveAction = {
                        performWithDriveAccess(action)
                    }
                    val intentSenderRequest = IntentSenderRequest.Builder(result.pendingIntent!!.intentSender).build()
                    authorizationLauncher.launch(intentSenderRequest)
                } else {
                    val token = result.accessToken
                    if (token != null) {
                        viewModel.fetchDriveBackupInfo(token)
                        action(token)
                    } else {
                        scope.launch { snackbarHostState.showSnackbar("Failed to get access token") }
                    }
                }
            }
            .addOnFailureListener { e ->
                scope.launch { snackbarHostState.showSnackbar("Drive access failed: ${e.message}") }
            }
    }

    LaunchedEffect(isGoogleSignedIn) {
        if (isGoogleSignedIn) {
            performWithDriveAccess { /* just to fetch info */ }
        }
    }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
        onResult = { uri ->
            uri?.let {
                isLoading = true
                viewModel.backupDatabase(
                    uri = it,
                    onSuccess = {
                        isLoading = false
                        scope.launch {
                            snackbarHostState.showSnackbar("Backup successful")
                        }
                    },
                    onError = { message ->
                        isLoading = false
                        scope.launch {
                            snackbarHostState.showSnackbar("Backup failed: $message")
                        }
                    }
                )
            }
        }
    )

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                isLoading = true
                viewModel.restoreDatabase(
                    uri = it,
                    onSuccess = {
                        isLoading = false
                        showRestoreSuccessDialog = true
                    },
                    onError = { message ->
                        isLoading = false
                        scope.launch {
                            snackbarHostState.showSnackbar("Restore failed: $message")
                        }
                    }
                )
            }
        }
    )

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { 
            viewModel.exportToCsv(
                uri = it,
                context = context,
                onSuccess = {
                    scope.launch { snackbarHostState.showSnackbar("Transactions exported successfully") }
                },
                onError = { error ->
                    scope.launch { snackbarHostState.showSnackbar("Export failed: $error") }
                }
            )
        }
    }

    val importCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.importFromCsv(
                uri = it,
                context = context,
                onSuccess = {
                    scope.launch { snackbarHostState.showSnackbar("Transactions imported successfully") }
                },
                onError = { error ->
                    scope.launch { snackbarHostState.showSnackbar("Import failed: $error") }
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Backup & Restore", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text(
                            "Keep your financial data safe and portable", 
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isGoogleSignedIn) {
                        Box {
                            IconButton(onClick = { showMoreOptions = true }) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = "More options", tint = Color.White)
                            }
                            DropdownMenu(
                                expanded = showMoreOptions,
                                onDismissRequest = { showMoreOptions = false },
                                modifier = Modifier.background(Color(0xFF1F2937))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Delete Cloud Data", color = Color.Red.copy(alpha = 0.8f)) },
                                    leadingIcon = { Icon(Icons.Rounded.DeleteSweep, contentDescription = null, tint = Color.Red.copy(alpha = 0.8f)) },
                                    onClick = { 
                                        showMoreOptions = false
                                        showDeleteConfirmDialog = true 
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sign Out", color = Color.White) },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, tint = Color.White) },
                                    onClick = { 
                                        showMoreOptions = false
                                        scope.launch {
                                            viewModel.authHelper.signOut()
                                            viewModel.setGoogleSignedIn(false)
                                        }
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Black
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(vertical = 20.dp)
            ) {
                // Header Status Chip
                if (isGoogleSignedIn) {
                    item {
                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(100.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(8.dp).background(Color(0xFF10B981), androidx.compose.foundation.shape.CircleShape))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Drive connected", style = MaterialTheme.typography.labelSmall, color = Color(0xFF10B981))
                            }
                        }
                    }
                }

                // Google Drive Primary Section
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                        shape = RoundedCornerShape(28.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Cloud, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Google Drive", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                    if (googleAccountEmail != null) {
                                        Text(googleAccountEmail!!, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))

                            if (!isGoogleSignedIn) {
                                Button(
                                    onClick = { 
                                        scope.launch {
                                            val credential = viewModel.authHelper.signInWithGoogle(context)
                                            if (credential != null) {
                                                viewModel.setGoogleSignedIn(true, credential.id)
                                                snackbarHostState.showSnackbar("Signed in as ${credential.id}")
                                            } else {
                                                snackbarHostState.showSnackbar("Sign in failed")
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                                    shape = RoundedCornerShape(16.dp),
                                    enabled = !isLoading
                                ) {
                                    Text("Sign in with Google", modifier = Modifier.padding(vertical = 6.dp))
                                }
                            } else {
                                // Last Backup Info
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text("Last backup", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text(
                                        text = if (driveBackupTimestamp != null) dateFormat.format(Date(driveBackupTimestamp!!)) else "No backup found",
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                        color = Color.White
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(if (isBackupUpToDate) Color(0xFF10B981) else Color(0xFFF59E0B), androidx.compose.foundation.shape.CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            if (isBackupUpToDate) "Backup is up to date" else "Backup recommended",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isBackupUpToDate) Color(0xFF10B981) else Color(0xFFF59E0B)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(28.dp))

                                Button(
                                    onClick = { 
                                        performWithDriveAccess { token ->
                                            isLoading = true
                                            viewModel.backupToGoogleDrive(
                                                accessToken = token,
                                                onSuccess = {
                                                    isLoading = false
                                                    viewModel.fetchDriveBackupInfo(token)
                                                    scope.launch { snackbarHostState.showSnackbar("Backup successful") }
                                                },
                                                onError = { message ->
                                                    isLoading = false
                                                    scope.launch { snackbarHostState.showSnackbar("Backup failed: $message") }
                                                }
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                                    shape = RoundedCornerShape(16.dp),
                                    enabled = !isLoading
                                ) {
                                    Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Backup now", modifier = Modifier.padding(vertical = 6.dp))
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedButton(
                                    onClick = { 
                                        performWithDriveAccess { token ->
                                            isLoading = true
                                            viewModel.restoreFromGoogleDrive(
                                                accessToken = token,
                                                onSuccess = {
                                                    isLoading = false
                                                    showRestoreSuccessDialog = true
                                                },
                                                onError = { message ->
                                                    isLoading = false
                                                    scope.launch { snackbarHostState.showSnackbar("Restore failed: $message") }
                                                }
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF374151)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    enabled = !isLoading
                                ) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Restore backup", modifier = Modifier.padding(vertical = 6.dp))
                                }
                                
                                Spacer(modifier = Modifier.height(20.dp))
                                
                                HorizontalDivider(color = Color(0xFF1F2937))
                                
                                Row(
                                    modifier = Modifier.padding(top = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Automatic backup", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                                        Text("Daily cloud backup", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                    Switch(
                                        checked = preferences.automaticBackupEnabled,
                                        onCheckedChange = { viewModel.updateAutomaticBackupEnabled(it, context) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Color(0xFF7C3AED),
                                            uncheckedThumbColor = Color.Gray,
                                            uncheckedTrackColor = Color(0xFF1F2937)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Local Backup Section
                item {
                    Column {
                        Text(
                            "LOCAL BACKUP", 
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), 
                            color = Color.Gray, 
                            modifier = Modifier.padding(start = 12.dp, bottom = 12.dp)
                        )
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                            shape = RoundedCornerShape(28.dp)
                        ) {
                            Column {
                                ExportImportOption(
                                    title = "Export backup",
                                    description = "Complete app backup file",
                                    icon = Icons.Rounded.Download,
                                    onClick = { 
                                        val fileName = "RupeeFlow_Backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.db"
                                        backupLauncher.launch(fileName)
                                    }
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = Color(0xFF1F2937).copy(alpha = 0.5f))
                                ExportImportOption(
                                    title = "Import backup",
                                    description = "Restore from a .db file",
                                    icon = Icons.Rounded.Upload,
                                    onClick = { 
                                        restoreLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                                    }
                                )
                            }
                        }
                    }
                }

                // Data Portability Section
                item {
                    Column {
                        Text(
                            "DATA PORTABILITY", 
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), 
                            color = Color.Gray, 
                            modifier = Modifier.padding(start = 12.dp, bottom = 12.dp)
                        )
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                            shape = RoundedCornerShape(28.dp)
                        ) {
                            Column {
                                ExportImportOption(
                                    title = "Export transactions",
                                    description = "CSV • Excel compatible",
                                    icon = Icons.Rounded.TableChart,
                                    onClick = { exportCsvLauncher.launch("rupeeflow_transactions.csv") }
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = Color(0xFF1F2937).copy(alpha = 0.5f))
                                ExportImportOption(
                                    title = "Import transactions",
                                    description = "From CSV file",
                                    icon = Icons.Rounded.UploadFile,
                                    onClick = { importCsvLauncher.launch(arrayOf("text/csv")) }
                                )
                            }
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        "Your data remains private. Google Drive backups are stored in your app's restricted application folder.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4B5563),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
            
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF7C3AED))
                }
            }

            if (showRestoreSuccessDialog) {
                AlertDialog(
                    onDismissRequest = { /* Don't dismiss */ },
                    title = { Text("Restore Successful") },
                    text = { Text("The database has been restored. The app needs to restart to apply the changes.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                                val mainIntent = android.content.Intent.makeRestartActivityTask(intent?.component)
                                context.startActivity(mainIntent)
                                Runtime.getRuntime().exit(0)
                            }
                        ) {
                            Text("Restart Now")
                        }
                    },
                    containerColor = Color(0xFF111827),
                    titleContentColor = Color.White,
                    textContentColor = Color.Gray
                )
            }

            if (showDeleteConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmDialog = false },
                    title = { Text("Delete Cloud Data?") },
                    text = { Text("This will permanently delete your backup from Google Drive. This action cannot be undone.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDeleteConfirmDialog = false
                                performWithDriveAccess { token ->
                                    isLoading = true
                                    viewModel.deleteGoogleDriveBackup(
                                        accessToken = token,
                                        onSuccess = {
                                            isLoading = false
                                            scope.launch { snackbarHostState.showSnackbar("Cloud data deleted successfully") }
                                        },
                                        onError = { message ->
                                            isLoading = false
                                            scope.launch { snackbarHostState.showSnackbar("Delete failed: $message") }
                                        }
                                    )
                                }
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                        ) {
                            Text("Delete Everything")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirmDialog = false }) {
                            Text("Cancel", color = Color.Gray)
                        }
                    },
                    containerColor = Color(0xFF111827),
                    titleContentColor = Color.White,
                    textContentColor = Color.Gray
                )
            }
        }
    }
}

@Composable
fun ExportImportOption(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                Text(description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}
